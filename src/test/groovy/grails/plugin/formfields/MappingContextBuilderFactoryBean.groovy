package grails.plugin.formfields

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.FactoryBean

/**
 * Created by jameskleeh on 5/3/17.
 */
class MappingContextBuilderFactoryBean implements FactoryBean<MappingContext>, GrailsApplicationAware {

    GrailsApplication grailsApplication
    Class[] domains = [] as Class[]

    @Override
    MappingContext getObject() throws Exception {
        new MappingContextBuilder(grailsApplication).build(domains)
    }

    @Override
    Class<?> getObjectType() {
        MappingContext
    }

    @Override
    boolean isSingleton() {
        true
    }
}
