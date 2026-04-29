
# Phase 2
## Introduction

This document describes the requirements for the second phase of the Software Laboratory project.
## Requirements
### Additional Operations

    Get a list with the bookings of a user
    Delete a booking
    Update a booking
        date interval - booking date interval

### In-Memory Cache in server (Optional)

As an optional enhancement, the system may include an in-memory cache to improve performance when retrieving house details.

The cache should store the results of the last N accesses to house details.

    If the information is already in the cache, it should be returned directly from the cache.
    Otherwise, the system should retrieve the information normally and store it in the cache.

The value of N should be configurable.

Note: The proposed caching solution must be discussed with the teacher before implementation.

### Single Page Application

The main requirement for the second phase is the delivery of a Single Page Application (SPA) to provide a Web User Interface to the GET operations developed in the first phase.

In this repository there are two examples of simple Single Page Applications:

    A simple one
    One more complete using a router

To see these two examples working, you must launch the HTTPServer application on file pt.isel.ls.http.HttpServer.kt and go to the following URI on your web browser: http://localhost:9000/

Note that the routes in file pt.isel.ls.http.HttpServer.kt have been updated.

Create HTML views that contain links to ensure the navigability defined in the following graph, changes to the graph should be discuss with the teacher:

Navigation

Note: All views must have a link to Home


To simplify the creation of HTML views, students should implement a small Domain-Specific Language (DSL) in JavaScript for building HTML elements.

Instead of repeatedly using low-level DOM APIs such as document.createElement, the DSL should provide a more declarative and composable way to construct the UI.

For example, rather than:
```
const div = document.createElement("div")
div.appendChild(document.createTextNode("Hello"))
```

Students could define helper functions such as:
```
div("Hello")
```

or more structured compositions:
```
div(
  h1("Title"),
  p("Some text")
)
```
The goal is to improve readability, reduce boilerplate, and make view construction more expressive.

### Report

The technical report created for phase 1 should be updated and/or extended with the relevant technical information. The sections developed for phase 1 can be improved or changed. There should not be a separate report for phase 2. The goal is to have a single report, updated through all the project phases.

There isn't any template for phase 2.

### Delivery date

The phase 2 must be delivered until 25 of April, through the creation of the 0.2.0 tag on the group´s GitHub repository.
