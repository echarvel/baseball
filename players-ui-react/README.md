# Player UI

Player UI is a react based web app that displays summary content of players.

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Technology
- React
- NPM
- Node.js (version 18.20.2)
  - Download and install from [nodejs.org](https://nodejs.org/)
  - Verify installation: `node --version`
- Css

## Overview

- `index.js` is the entry point file
- `components` folder contains child components
- `assets` folder contains any needed static content
- `styling` folder contains css files
- `utils` folder contains any needed utility functions
- `tests` folder contains unit tests for the project

## Available Scripts

In the project directory, you can run:

### `npm install`

Installs all the dependencies required for the project to run.\

### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.\
You may also see any lint errors in the console.

### `npm test`

Launches the test runner in the interactive watch mode.\
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### `npm run build`

Builds the app for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.
The build is minified and the filenames include the hashes.\
Your app is ready to be deployed!

## Calling local service from react app

React app is linked to local service using a proxy parameter in the package.json.
