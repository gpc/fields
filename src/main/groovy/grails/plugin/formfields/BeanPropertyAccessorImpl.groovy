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

import grails.gorm.Entity
import grails.util.GrailsNameUtils
import grails.web.databinding.WebDataBinding
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.apache.commons.lang.ClassUtils
import grails.core.*
import grails.plugins.VersionComparator
import grails.validation.ConstrainedProperty
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.springframework.validation.FieldError

import static grails.validation.ConstrainedProperty.BLANK_CONSTRAINT

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
        grailsApplication.config.getProperty("grails.databinding.$paramName", Boolean, defaultParamValue)
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
            boolean hasBlankConstraint = constraints?.hasAppliedConstraint(BLANK_CONSTRAINT)
            boolean blanksImplicityProhibited = !hasBlankConstraint && !constraints?.nullable && convertBlanksToNull
			!constraints?.nullable && (!constraints?.blank || blanksImplicityProhibited)
		} else {
			!constraints?.nullable
		}
	}

	boolean isInvalid() {
		!errors.isEmpty()
	}

	@CompileStatic
	private List<Class> getSuperclassesAndInterfaces(Class type) {
		List<Class> superclasses = []
		superclasses.addAll(ClassUtils.getAllSuperclasses(ClassUtils.primitiveToWrapper(type)))
		for(Object it in ClassUtils.getAllInterfaces(type)) {
			Class interfaceCls = (Class)it
			String name = interfaceCls.name
			if(name.indexOf('$') == -1) {
				if(interfaceCls.package != GormEntity.package) {
					superclasses.add(interfaceCls)
				}
			}
		}
		superclasses.removeAll([Object, GroovyObject, Serializable, Cloneable, Comparable, WebDataBinding, DirtyCheckable, Entity])
		return superclasses.unique()
	}
}
