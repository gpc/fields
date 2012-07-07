package grails.plugin.formfields

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

public enum AssociationType {

	oneToMany, manyToOne, manyToMany, oneToOne

	static AssociationType forProperty(GrailsDomainClassProperty property) {
		if (!property.association) return null
		else if (property.oneToMany) return oneToMany
		else if (property.oneToOne) return oneToOne
		else if (property.manyToOne) return manyToOne
		else if (property.manyToMany) return manyToMany
		else throw new IllegalStateException('unknown association type')
	}
}