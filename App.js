// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar, DrawerLayoutAndroid, Text, AsyncStorage } from 'react-native';

import { DrawerNavigator } from 'react-navigation';

import { styles } from './src/style';
import { MiaoliMenu, PageTribune } from './src/tribune';

export default class App extends React.Component {
  constructor(props) {
    super(props)

    this.state = {
      configurationLoaded: false,
      configuration: [],
    };
  }

  defaultSettings = () => {
    return [
      {
        title: 'Moules',
        color: 'blue',
        backend: 'http://moules.org/board/last.php?backend=tsv',
        post_url: 'http://moules.org/board/add.php',
        post_format: 'message=%s',
        user_agent: 'Miaoli/0.0',
      },
      {
        title: 'Euromussels',
        color: 'blue',
        backend: 'http://faab.euromussels.eu/data/backend.xml',
        post_url: 'http://faab.euromussels.eu/add.php',
        post_format: 'message=%s',
        user_agent: 'Miaoli/0.0',
      },
      {
        title: 'Adonai',
        color: 'blue',
        backend: 'http://miaoli.im/tribune/papitalisme/tsv',
        post_url: 'http://miaoli.im/tribune/papitalisme/post',
        post_format: 'message=%s',
        user_agent: 'Miaoli/0.0',
      },
    ];
  }

  componentDidMount = () => {
    AsyncStorage
      .getItem("tribune:configuration")
      .then((result) => {

        if (true || !result) {
          AsyncStorage.setItem('tribune:configuration', JSON.stringify(this.defaultSettings()))
          this.setState({configurationLoaded: true, configuration: this.defaultSettings()})
        } else {
          this.setState({configurationLoaded: true, configuration: JSON.parse(result)})
        }
      })
  }

  render() {
    const displayNavigation = this.state.configurationLoaded ? 'flex' : 'none';
    const displayLoading = this.state.configurationLoaded ? 'none' : 'flex';

    if (this.state.configurationLoaded) {
      var screens = {}
      this.state.configuration.forEach((tribune, i) => {
        screens["tribune-" + i] = {
          screen: PageTribune,
        }
      })

      const NavigationDrawer = DrawerNavigator(screens);

      return (
         <View style={{flex: 1}}>
           <NavigationDrawer style={{display: displayNavigation}} screenProps={{configuration: this.state.configuration}} />
         </View>
      );
    } else {
      return (
         <View style={{flex: 1}}>
           <Text>Loading configuration...</Text>
         </View>
      );
    }
  }
}
