// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar, DrawerLayoutAndroid, Text } from 'react-native';

import { StackNavigator, addNavigationHelpers } from 'react-navigation';

import { styles } from './src/style';
import { MiaoliMenu, PageTribune, PageSettings } from './src/tribune';

const NavigationStack = StackNavigator({
  Home: {
    screen: PageTribune
  },
  Settings: {
    screen: PageSettings,
    path: 'settings',
  },
});

export default class App extends React.Component {
  navigationView = () => {
    return ( <MiaoliMenu navigation={this.navRef} drawer={this.drawerRef} /> );
  }

  render() {
    return (
      <DrawerLayoutAndroid
        ref={(ref) => this.drawerRef = ref}
        drawerWidth={300}
        drawerPosition={DrawerLayoutAndroid.positions.Left}
        renderNavigationView={() => this.navigationView()}>
          <NavigationStack ref={(ref) => this.navRef = ref} />
      </DrawerLayoutAndroid>
    );
  }
}
