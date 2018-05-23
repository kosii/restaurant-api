# restaurant-api

It's a little REST API to manage your favourite restaurants with the help of [akka-http]() and [levelDB](http://leveldb.org/).


# Run

The easiest way to run the project is through docker. Either you can build it for yourself if you have `sbt` and `docker` installed on your machine, or you can just pull the published image from the public docker repository.

## Getting the image

### Build it yourself

With the sbt-native-packager plugin you can build a docker image with just one simple command:

    sbt docker:publishLocal
    
### Pull the published image

If you don't have sbt installed on your PC, it can be easier just to pull the public image:

    docker pull docker.io/kosii/restaurant-api:0.1

## Running the image

The default way to run the image 

    docker run -p 8080:8080 kosii/restaurant-api:0.1
    
To change the port to expose the http service on your host please refer to the docker documentation.

# Interacting with the service

# POST /restaurants

Create a restaurant. The restaurant's id will be determined by the service. It'll return 201 if the restaurant was correctly created, and the `Location` header will point to the newly created resource.

## Example

    $ curl -v -X POST -H "Content-Type: application/json" -d '{"name":"Le Petit Poucet","phoneNumber":"+33 1 47 38 61 85","cuisines":["french","traditional"],"address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France","description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"}' http://localhost:8080/restaurants
    
    > POST /restaurants HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.54.0
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 432
    >
    < HTTP/1.1 201 Created
    < Location: /restaurants/b225a3f3-f92a-430a-b3f3-8f814e6267ae
    < Server: akka-http/10.1.1
    < Date: Wed, 23 May 2018 13:52:13 GMT
    < Content-Length: 0
    <
    
# PUT /restaurants/\<uuid\>

This request will create or update a restaurant with a given id. It's unnecessary to provide an id in the request's body, it's always the id in the url which will be taken in account. It'll return 204 if the restarant was correctly created or updated.

## Example

    $ curl -v -X PUT -H "Content-Type: application/json" -d '{"name":"Le Petit Poucet","phoneNumber":"+33 1 47 38 61 85","cuisines":["french","traditional"],"address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France","description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"}' http://localhost:8080/restaurants/a4fc27ac-beaa-4ece-97b0-349b43127475
    
    > PUT /restaurants/a4fc27ac-beaa-4ece-97b0-349b43127475 HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.54.0
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 432
    >
    < HTTP/1.1 204 No Content
    < Server: akka-http/10.1.1
    < Date: Wed, 23 May 2018 13:51:27 GMT
    <


# GET /restaurants/\<uuid\>

Return the restaurant associated with the given id. Returns 404 if the restaurant doesn't exist.

## Example
    
    $ curl -v -X GET http://localhost:8080/restaurants/a4fc27ac-beaa-4ece-97b0-349b43127475

    > GET /restaurants/a4fc27ac-beaa-4ece-97b0-349b43127475 HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.54.0
    > Accept: */*
    >
    < HTTP/1.1 200 OK
    < Server: akka-http/10.1.1
    < Date: Wed, 23 May 2018 13:49:14 GMT
    < Content-Type: application/json
    < Content-Length: 476
    <
    [{"id":"a4fc27ac-beaa-4ece-97b0-349b43127475","name":"Le Petit Poucet","phoneNumber":"+33 1 47 38 61 85","cuisines":["french","traditional"],"address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France","description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"}]


# DELETE /restaurants/\<uuid\>

Delete an already exising restaurant. If the restaurant doesn't exist, it'll return 404. It'll return 204 if the restaurant was correctly deleted.


## Example 

    $ curl -v -X DELETE localhost:8080/restaurants/a4fc27ac-beaa-4ece-97b0-349b43127475
    > DELETE /restaurants/a4fc27ac-beaa-4ece-97b0-349b43127475 HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.54.0
    > Accept: */*
    >
    < HTTP/1.1 204 No Content
    < Server: akka-http/10.1.1
    < Date: Wed, 23 May 2018 13:55:24 GMT
    <
# GET /restaurants

Returns the complete list of the restaurants.


## Example 
    
    $ curl -v -X GET http://localhost:8080/restaurants
    > GET /restaurants HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.54.0
    > Accept: */*
    >
    < HTTP/1.1 200 OK
    < Server: akka-http/10.1.1
    < Date: Wed, 23 May 2018 13:54:13 GMT
    < Content-Type: application/json
    < Content-Length: 478
    <
    [{"id":"a4fc27ac-beaa-4ece-97b0-349b43127475","name":"Le Petit Poucet","phoneNumber":"+33 1 47 38 61 85","cuisines":["french","traditional"],"address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France","description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"}]
    

# GET /v1/healthcheck

A simple request to make sure that the service is still up and running

## Example

    $ curl -v -X GET http://localhost:8080/v1/healthcheck
    > GET /v1/healthcheck HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.54.0
    > Accept: */*
    >
    < HTTP/1.1 200 OK
    < Server: akka-http/10.1.1
    < Date: Wed, 23 May 2018 13:56:22 GMT
    < Content-Type: text/plain; charset=UTF-8
    < Content-Length: 2
    <