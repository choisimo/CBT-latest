/**
 * @format
 */

import {AppRegistry} from 'react-native';
import App from './App';
import {name as appName} from './app.json';

AppRegistry.registerComponent(appName, () => App);

// Web support - React Native Web
if (typeof document !== 'undefined') {
  const rootElement = document.getElementById('root');
  if (rootElement) {
    AppRegistry.runApplication(appName, {
      initialProps: {},
      rootTag: rootElement,
    });
  }
}
