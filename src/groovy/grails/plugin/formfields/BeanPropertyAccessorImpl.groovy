/*
 * Copyright 2012 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.formfields

import grails.util.GrailsNameUtils
import groovy.transform.Canonical
import org.apache.commons.lang.ClassUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.VersionComparator
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError

import static org.codehaus.groovy.grails.validation.ConstrainedProperty.BLANK_CONSTRAINT

@Canonical(includes = 'beanType, propertyName, propertyType')
class BeanPropertyAccessorImpl implements BeanPropertyAccessor {
	
	Object rootBean
	GrailsDomainClass rootBeanClass
	Class rootBeanType
	GrailsDomainClass beanClass
	Class beanType
	String pathFromRoot
	String propertyName
	Class propertyType
	GrailsDomainClassProperty persistentProperty
	ConstrainedProperty constraints
	Object value
	GrailsApplication grailsApplication

    /**
     * Since Grails 2.3 blank values that are provided for String properties are
     * <a href="http://grails.1312388.n4.nabble.com/Grails-2-3-Data-Binding-String-Trimming-And-Null-Conversions-td4645255.html">converted to null by default</a>
     */
    @Lazy
    private boolean convertBlanksToNull = { ->

        String applicationGrailsVersion = grailsApplication.metadata.getGrailsVersion()
        boolean isAtLeastGrails2Point3 = new VersionComparator().compare(applicationGrailsVersion, '2.3') != -1

        if (isAtLeastGrails2Point3) {
            getDataBindingConfigParamValue('convertEmptyStringsToNull') && getDataBindingConfigParamValue('trimStrings')
        } else {
            false
        }
    }()

    /**
     * Returns the effective value of a a boolean config param from the <code>grails.databinding</code> node
     * @param paramName
     * @param defaultParamValue if the param doesn't exist, use this as the default value
     * @return
     */
    private boolean getDataBindingConfigParamValue(String paramName, boolean defaultParamValue = true) {
        def paramValue = grailsApplication.config.grails.databinding."$paramName"
        paramValue ? paramValue as Boolean : defaultParamValue
    }

	List<Class> getBeanSuperclasses() {
		getSuperclassesAndInterfaces(beanType)
	}
	
	List<Class> getPropertyTypeSuperclasses() {
		getSuperclassesAndInterfaces(propertyType)
	}

	List<String> getLabelKeys() {
		[
			"${GrailsNameUtils.getPropertyName(rootBeanType.simpleName)}.${pathFromRoot}.label".replaceAll(/\[(.+)\]/, ''),
			"${GrailsNameUtils.getPropertyName(beanType.simpleName)}.${propertyName}.label"
		].unique()
	}

	String getDefaultLabel() {
		GrailsNameUtils.getNaturalName(propertyName)
	}

	List<FieldError> getErrors() {
		if (rootBean.metaClass.hasProperty(rootBean, 'errors') && rootBean.errors) {

			rootBean.errors.getFieldErrors(pathFromRoot)
		} else {
			[]
		}
	}

	boolean isRequired() {
		if (propertyType in [Boolean, boolean]) {
			false
		} else if (propertyType == String) {
            // if the property prohibits nulls and blanks are converted to nulls, then blanks will be prohibited even if a blank
            // constraint does not exist
            boolean hasBlankConstraint = constraints.hasAppliedConstraint(BLANK_CONSTRAINT)
            boolean blanksImplicityProhibited = !hasBlankConstraint && !constraints?.nullable && convertBlanksToNull
			!constraints?.nullable && (!constraints?.blank || blanksImplicityProhibited)
		} else {
			!constraints?.nullable
		}
	}

	boolean isInvalid() {
		!errors.isEmpty()
	}

	private List<Class> getSuperclassesAndInterfaces(Class type) {
		def superclasses = []
		superclasses.addAll(ClassUtils.getAllSuperclasses(ClassUtils.primitiveToWrapper(type)))
		superclasses.addAll(ClassUtils.getAllInterfaces(type))
		superclasses.removeAll([Object, GroovyObject, Serializable, Cloneable, Comparable])
		superclasses
	}
}
