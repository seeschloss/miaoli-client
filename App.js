// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar, DrawerLayoutAndroid, Text } from 'react-native';

import { DrawerNavigator } from 'react-navigation';

import { styles } from './src/style';
import { MiaoliMenu, PageTribune, PageSettings } from './src/tribune';

const NavigationDrawer = DrawerNavigator({
  'Tribune-Moules': {
    screen: PageTribune,
  },
  'Tribune-Moules 2': {
    screen: PageTribune,
  },
  Settings: {
    screen: PageSettings,
    path: 'settings',
  },
});

export default class App extends React.Component {
  render() {
    return (
       <NavigationDrawer />
    );
  }
}
