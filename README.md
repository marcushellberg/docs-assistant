# An AI Chatbot for Vaadin Flow and Hilla docs

This app is a simple chatbot that uses [Vaadin Flow](https://vaadin.com/flow) and [Hilla](https://hilla.dev) docs as context.

The app assumes you have created embeddings with [Vaadin Docs Embeddings](https://github.com/marcushellberg/vaadin-docs-embeddings).


## Requirement

* Java 17+

## Environment variables

You need to have the following environment variables defined to run the app:

* `OPENAI_API_KEY`: Your OpenAI API key
* `PINECONE_API_KEY`: Your Pinecone API key
* `PINECONE_API_URL`: Your Pinecone API URL

## Running the app

Start the app by running `Application.java`, or with the default Maven goal:

```
mvn
```