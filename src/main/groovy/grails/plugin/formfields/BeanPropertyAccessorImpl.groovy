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

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.gorm.Entity
import grails.gorm.validation.ConstrainedProperty
import grails.plugins.VersionComparator
import grails.util.GrailsNameUtils
import grails.web.databinding.WebDataBinding
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.apache.commons.lang.ClassUtils
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.scaffolding.model.property.Constrained
import org.springframework.validation.FieldError

@Canonical
@TupleConstructor(includes = ['beanType', 'propertyName', 'propertyType'])
class BeanPropertyAccessorImpl implements BeanPropertyAccessor {

    Object rootBean
    Class rootBeanType
    GrailsDomainClass beanClass
    Class beanType
    String pathFromRoot
    String propertyName
    Class propertyType
    Constrained constraints
    Object value
    PersistentProperty domainProperty
    PersistentEntity entity
    GrailsApplication grailsApplication

    @Lazy
    private boolean convertBlanksToNull = { ->
        getDataBindingConfigParamValue('convertEmptyStringsToNull') && getDataBindingConfigParamValue('trimStrings')
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

    private boolean getAddPathFromRoot() {
        grailsApplication.config.getProperty('grails.plugin.fields.i18n.addPathFromRoot', Boolean, false)
    }

    List<Class> getBeanSuperclasses() {
        getSuperclassesAndInterfaces(beanType)
    }

    List<Class> getPropertyTypeSuperclasses() {
        getSuperclassesAndInterfaces(propertyType)
    }

    List<String> getLabelKeys() {
        List<String> labelKeys = []
        
        labelKeys << "${GrailsNameUtils.getPropertyName(rootBeanType.simpleName)}.${pathFromRoot}.label".replaceAll(/\[(.+)\]/, '') 
        if(addPathFromRoot) {
            labelKeys << "${pathFromRoot}.label".replaceAll(/\[(.+)\]/, '')
        }                
        labelKeys << "${GrailsNameUtils.getPropertyName(beanType.simpleName)}.${propertyName}.label".toString()

        return labelKeys.unique()
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
            boolean hasBlankConstraint = constraints?.hasAppliedConstraint(ConstrainedProperty.BLANK_CONSTRAINT)
            boolean blanksImplicitlyProhibited = !hasBlankConstraint && !constraints?.nullable && convertBlanksToNull
            !constraints?.nullable && (!constraints?.blank || blanksImplicitlyProhibited)
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
        for (Object it in ClassUtils.getAllInterfaces(type)) {
            Class interfaceCls = (Class) it
            String name = interfaceCls.name
            if (name.indexOf('$') == -1) {
                if (interfaceCls.package != GormEntity.package) {
                    superclasses.add(interfaceCls)
                }
            }
        }
        superclasses.removeAll([Object, GroovyObject, Serializable, Cloneable, Comparable, WebDataBinding, DirtyCheckable, Entity])
        return superclasses.unique()
    }
}
