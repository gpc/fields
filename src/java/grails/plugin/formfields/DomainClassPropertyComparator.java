package grails.plugin.formfields;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.Map;

/**
 * Comparator that uses the domain class property constraints to establish order in sort methods and always
 * places the id first.
 *
 * This class is a copy of org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator which
 * was moved into the scaffolding plugin in Grails 2.3.0. When the plugin is upgraded to Grails >= 2.3.0
 * this class should be removed from the plugin and all usages of it should be replaced with
 * org.codehaus.groovy.grails.validation.DomainClassPropertyComparator.
 *
 * @author Graeme Rocher
 * @deprecated Use org.codehaus.groovy.grails.validation.DomainClassPropertyComparator instead
 */
@SuppressWarnings("rawtypes")
@Deprecated
public class DomainClassPropertyComparator implements Comparator {

    private Map constrainedProperties;
    private GrailsDomainClass domainClass;

    public DomainClassPropertyComparator(GrailsDomainClass domainClass) {
        Assert.notNull(domainClass, "Argument 'domainClass' is required!");

        constrainedProperties = domainClass.getConstrainedProperties();
        this.domainClass = domainClass;
    }

    public int compare(Object o1, Object o2) {
        if (o1.equals(domainClass.getIdentifier())) {
            return -1;
        }
        if (o2.equals(domainClass.getIdentifier())) {
            return 1;
        }

        GrailsDomainClassProperty prop1 = (GrailsDomainClassProperty)o1;
        GrailsDomainClassProperty prop2 = (GrailsDomainClassProperty)o2;

        ConstrainedProperty cp1 = (ConstrainedProperty)constrainedProperties.get(prop1.getName());
        ConstrainedProperty cp2 = (ConstrainedProperty)constrainedProperties.get(prop2.getName());

        if (cp1 == null & cp2 == null) {
            return prop1.getName().compareTo(prop2.getName());
        }

        if (cp1 == null) {
            return 1;
        }

        if (cp2 == null) {
            return -1;
        }

        if (cp1.getOrder() > cp2.getOrder()) {
            return 1;
        }

        if (cp1.getOrder() < cp2.getOrder()) {
            return -1;
        }

        return 0;
    }
}
