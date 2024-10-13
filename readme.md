# Fields plugin

A spiritual successor to the bean-fields plugin that attempts to provide a configurable way to render forms with appropriate inputs for different properties without having to copy and paste lots of boilerplate code. It should be possible to change the rendering for a field with the minimum of impact on any other code. This plugin attempts to achieve that by using GSP templates looked up by convention. Developers can then create templates for rendering particular properties or types of properties with the former overriding the latter.

## Documentation: 
For further information please see the full documentation.

Documentation can be found [here](https://gpc.github.io/fields)


## Tags and branches: 
- `master` latest version for Grails 7.x `implementation "org.grals.plugins:fields:7.0.0"`
- `5.1.0` latest version for Grails 5.x `implementation "org.grals.plugins:fields:5.1.0"` - Also works with Grails 6.x
- `4.0.0` latest version for Grails 4.x `compile "io.github.gpc:fields:4.0.0"`
- `3.0.0.RC1` last version on coordinates: `org.grails.plugins:fields:3.0.0.RC` - Grails 4.x (no support)
- `2.2.x` and `2.1.x` for Grails 3. (no support)
- `grails2.x` for Grails 2. (no support)


## Important 

If you use `org.grails.plugins:scaffolding` version 4.10.0 or less you need to exclude the original `org.grals.plugins:fields:3.3.0.RC1` like this:

```
implementation("org.grails.plugins:scaffolding") {
  exclude module: 'fields'
}
implementation 'io.github.gpc:fields:5.0.0'
```

if you are using Grails 4.x, replace `implementation` with `compile` and use `io.github.gpc:fields:4.0.0`. 
