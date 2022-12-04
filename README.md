# Comperio - lightweight service discovery tool

Comperio borrowed its name from Latin which literally means "discover" or "find out" because that is exactly what it does.

## System Components
Comperio offers discovery via an HTTP server (REST API) (soon to come), gRPC (soon to come) and via embedded DNS server (soon to come).

Replication - Comperio was designed for high availability and therefore, we made it sure that the data has to propagate to other nodes as soon as possible so that the cluster can withstand the loss of several nodes. For this particular purpose Gossiping protocol has been chosen that provides a best effort delivery at low costs. The whole architecture is completely peer-to-peer without leaders.

# Requirements 
- Java 19
- Maven


# Registry HTTP Rest API v1 Overview
- Client Registration
<br/> <p>Registers client or updates the registration of the existing client. When existing client is present, its status is changed to COLD and number of renewals becomes 1.</p> <br/>
``POST /v1/applications/{appId}/instances/{instanceId}`` <br/><br/>
    - Body 
  ```
   {
      "host": "http://localhost",
      "port": 8080,
      "ipAddress": "127.0.0.1"
    } 
    ```
  - Success Response (HTTP 200OK)
  ```
    {
      "application": "test-service",
      "instanceId": "instance01",
      "host": "http://localhost",
      "port": 8080,
      "ipAddress": "/127.0.0.1",
      "registrationTimestamp": 1670162489136,
      "lastRenewalTimestamp": 1670162489136,
      "renewalsSinceRegistration": 1,
      "status": "COLD"
    }
  ```
  - Responses
    - HTTP 200 OK - when registration succeeds
      <br/>
      <br/>

- Client de-registration
  <br/> <p>Removes instance registration from the registry</p> <br/>
  ``DELETE /v1/applications/{appId}/instances/{instanceId}`` <br/><br/>
    - Body
  ```
   null
  ```
    - Success Response (HTTP 204 NO CONTENT)
  ```
    null
  ```
    - Responses
        - HTTP 200 OK - when deletion succeeds
          <br/>
          <br/>
      
- Client HeartBeat
  <br/> <p>Increments renewal count by 1 for a client. Each client needs at least 3 heartbeats in order to be considered 'UP''</p> <br/>
  ``POST /v1/applications/{appId}/instances/{instanceId}/heartbeat`` <br/><br/>
    - Body
  ```
   null
  ```
    - Success Response (HTTP 204 NO CONTENT)
  ```
    null
  ```
    - Responses
        - HTTP 204 NO CONTENT - when renewal succeeds
        - HTTP 400 BAD REQUEST - when renewal fails due to client not being present
          <br/>
          <br/>

- Find all clients by application id
  <br/> <p>Returns the list of clients for an application</p> <br/>
  ``GET /v1/applications/{appId}/instances`` <br/><br/>
    - Body
  ```
   null
  ```
    - Success Response (HTTP 200 OK)
  ```
    [
      {
          "application": "test-service",
          "instanceId": "instance02",
          "host": "http://localhost",
          "port": 8080,
          "ipAddress": "/127.0.0.1",
          "registrationTimestamp": 1670164229557,
          "lastRenewalTimestamp": 1670164229557,
          "renewalsSinceRegistration": 1,
          "status": "COLD"
      },
      {
          "application": "test-service",
          "instanceId": "instance01",
          "host": "http://localhost",
          "port": 8080,
          "ipAddress": "/127.0.0.1",
          "registrationTimestamp": 1670164227165,
          "lastRenewalTimestamp": 1670164227165,
          "renewalsSinceRegistration": 1,
          "status": "COLD"
      }
    ]
  ```
    - Responses
        - HTTP 200 OK
          <br/>
          <br/>

- Find client by application id and instance id
  <br/> <p>Returns the client registration</p> <br/>
  ``GET /v1/applications/{appId}/instances/{instanceId}`` <br/><br/>
    - Body
  ```
   null
  ```
    - Success Response (HTTP 200 OK)
  ```
    {
      "application": "test-service",
      "instanceId": "instance01",
      "host": "http://localhost",
      "port": 8080,
      "ipAddress": "/127.0.0.1",
      "registrationTimestamp": 1670164337035,
      "lastRenewalTimestamp": 1670164337035,
      "renewalsSinceRegistration": 1,
      "status": "COLD"
    }
  ```
    - Responses
        - HTTP 200 OK
        - HTTP 404 NOT FOUND - when no instance found with such instanceId
          <br/>
          <br/>
