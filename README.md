# fhir-face

Universal frontend view to any sansara fhir server

## Overview

This application allows simple full-text search, CRUD and possible pretty view
for any fhir resource, containing in choosen sansara fhir server.
 
![alt text](https://user-images.githubusercontent.com/10473034/45783479-9199cb80-bc6e-11e8-959b-90f46b4c5a45.png "Reference graph view")

![alt text](https://user-images.githubusercontent.com/10473034/45785396-d0328480-bc74-11e8-8eaa-4fdfb54902b8.png "Resource grid view")

![alt text](https://user-images.githubusercontent.com/10473034/45785400-d4f73880-bc74-11e8-974b-e6616280ae87.png "Resource edit view")

![alt text](https://user-images.githubusercontent.com/10473034/45785406-dcb6dd00-bc74-11e8-9d57-35ab7462c8da.png "Resource edit view")


## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at `http://localhost:3449/?base-url=<url-of-your-sansara-fhir-server>#/resource`

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2018

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
