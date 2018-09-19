# fhir-face

Universal frontend view to any sansara fhir server

## Overview

This application allows simple full-text search, CRUD and possible pretty view
for any fhir resource, containing in choosen sansara fhir server.
 
![alt text](https://user-images.githubusercontent.com/10473034/45783479-9199cb80-bc6e-11e8-959b-90f46b4c5a45.png "Reference graph view")

![alt text](https://user-images.githubusercontent.com/10473034/45783489-978fac80-bc6e-11e8-9152-d6b9dde81a99.png "Resource grid view")

![alt text](https://user-images.githubusercontent.com/10473034/45783499-9e1e2400-bc6e-11e8-9704-4cb644ae4a8e.png "Resource edit view")

![alt text](https://user-images.githubusercontent.com/10473034/45783511-a5ddc880-bc6e-11e8-91e8-d902a9cce275.png "Resource edit view")


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
