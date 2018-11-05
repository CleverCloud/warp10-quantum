# Warp 10 Quantum

> Note: We have begun to do release notes for Warp 10 Quantum, please [read them](./RELEASE_NOTES.md) before upgrading 

Warp 10's Quantum is a web application developed to easily deal with the [Warp 10](https://www.warp10.io)
platform.

Entirely developed as [Polymer](https://www.polymer-project.org/) elements, you will
find in this repository not only the full application but the whole set of web-components
allowing to customize your Warp 10 experience.




## Contributing

The Warp 10 team loves contributions from the community! Issues and pull request are welcome.

## License

Warp 10's Quantum uses Apache 2.0 License, available [here](./LICENSE).

### Setup

#### Prerequisites

##### Install polymer-cli

Install [polymer-cli](https://github.com/Polymer/polymer-cli):

    npm install -g polymer-cli

> Note: You need to have `polymer-cli` in a version >= 0.18 in order to build Warp10 Quantum   

##### Get dependencies with Bower

After cloning the repository, you will need to use [Bower](http:/bower.io) to get
all the dependencies for the project (described in `bower.json` as usual).

    bower install

### Start the development server

This command serves the app at `http://localhost:8080` and provides basic URL
routing for the app:

    polymer serve


### Build

This command performs HTML, CSS, and JS minification on the application
dependencies, and generates a service-worker.js file with code to pre-cache the
dependencies based on the entrypoint and fragments specified in `polymer.json`.
The minified files are output to the `build/unbundled` folder, and are suitable
for serving from a HTTP/2+Push compatible server.

In addition the command also creates a fallback `build/bundled` folder,
generated using fragment bundling, suitable for serving from non
H2/push-compatible servers or to clients that do not support H2/Push.

    polymer build

### Test the build

This command serves the minified version of the app in an unbundled state, as it would
be served by a push-compatible server:

    polymer serve build/unbundled

This command serves the minified version of the app generated using fragment bundling:

    polymer serve build/bundled

### Extend

You can extend the app by adding more elements that will be demand-loaded
e.g. based on the route, or to progressively render non-critical sections
of the application.  Each new demand-loaded fragment should be added to the
list of `fragments` in the included `polymer.json` file.  This will ensure
those components and their dependencies are added to the list of pre-cached
components (and will have bundles created in the fallback `bundled` build).

