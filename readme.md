[![Build Status](https://travis-ci.org/grails-fields-plugin/grails-fields.svg?branch=master)](https://travis-ci.org/grails-fields-plugin/grails-fields)

A spiritual successor to the [bean-fields plugin](http://grails.org/plugin/bean-fields) that attempts to provide a configurable way to render forms with appropriate inputs for different properties without having to copy and paste lots of boilerplate code. It should be possible to change the rendering for a field with the minimum of impact on any other code. This plugin attempts to achieve that by using GSP templates looked up by convention. Developers can then create templates for rendering particular properties or types of properties with the former overriding the latter.

This work comes from [GRAILS-7635](http://jira.grails.org/browse/GRAILS-7635) and one of the major advantages would be to prolong the useful lifetime of dynamic scaffolding as developers can customize individual fields, add support for new property types, etc. whilst allowing scaffolding to continue laying out the overall page.

For further information please see the [full documentation](http://freeside.co/grails-fields).

