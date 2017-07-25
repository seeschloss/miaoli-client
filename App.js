// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar, DrawerLayoutAndroid, Text, AsyncStorage } from 'react-native';

import { DrawerNavigator, StackNavigator } from 'react-navigation';

import { styles } from './src/style';
import { MiaoliMenu, PageTribuneSettings, PageTribuneBrowser, PageTribuneLogin, Tribune } from './src/tribune';

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
        color: "#fde096",
        backend: 'http://moules.org/board/last.php?backend=tsv',
        backend_type: 'tsv',
        post_url: 'http://moules.org/board/add.php',
        post_format: 'message=%s',
        user_agent: 'Miaoli/0.0',
        cookie: '',
        loginpage: undefined,
      },
      {
        title: 'DLFP',
        color: "#67c6f2",
        backend: 'https://linuxfr.org/board/index.xml',
        backend_type: 'xml-htmlentitised',
        post_url: 'https://linuxfr.org/board',
        post_format: 'board[message]=%s',
        user_agent: 'Miaoli/0.0',
        cookie: '',
        loginpage: 'https://linuxfr.org',
      },
      {
        title: 'Euromussels',
        color: "#f4b189",
        backend: 'http://faab.euromussels.eu/data/backend.xml',
        backend_type: 'xml-raw',
        post_url: 'http://faab.euromussels.eu/add.php',
        post_format: 'message=%s',
        user_agent: 'Miaoli/0.0',
        cookie: '',
        loginpage: 'http://faab.euromussels.eu/loginF.php',
      },
      {
        title: 'Adonai',
        color: "#4bd0e3",
        backend: 'http://miaoli.im/tribune/papitalisme/tsv',
        backend_type: 'tsv',
        post_url: 'http://miaoli.im/tribune/papitalisme/post',
        post_format: 'message=%s',
        user_agent: 'Miaoli/0.0',
        cookie: '',
      },
    ];
  }

  componentDidMount = () => {
    AsyncStorage
      .getItem("tribune:configuration")
      .then((result) => {

        if (!result) {
          AsyncStorage.setItem('tribune:configuration', JSON.stringify(this.defaultSettings()))
          this.setState({configurationLoaded: true, configuration: this.defaultSettings()})
        } else {
          var defaultSettings = this.defaultSettings();
          var settings = JSON.parse(result);

          console.log(settings)
          for (var i in settings) {
            var backend = settings[i].backend

            for (var j in defaultSettings) {
              if (defaultSettings[j].backend == backend) {
                for (var key in defaultSettings[j]) {
                  if (settings[i][key] === undefined) {
                    settings[i][key] = defaultSettings[j][key]
                    console.log([backend, key, defaultSettings[j][key]])
                  } else if (key == "color" && settings[i][key] === "blue") {
                    settings[i][key] = defaultSettings[j][key]
                    console.log([backend, key, defaultSettings[j][key]])
                  }
                }
              }
            }
          }

          this.setState({configurationLoaded: true, configuration: settings})
        }
      })
  }

  render() {
    const displayNavigation = this.state.configurationLoaded ? 'flex' : 'none';
    const displayLoading = this.state.configurationLoaded ? 'none' : 'flex';

    if (this.state.configurationLoaded) {
      var screens = {}
      this.state.configuration.forEach((tribuneConfiguration, i) => {
        var tribuneObject = new Tribune(tribuneConfiguration)

        const NavigationStack = StackNavigator({
          TribuneHome: {
            screen: PageTribuneBrowser,
            path: 'tribune/:tribune/:page',
          },
          TribuneSettings: {
            screen: PageTribuneSettings,
            path: 'tribune/:tribune/:page/settings',
          },
          TribuneLogin: {
            screen: PageTribuneLogin,
            path: 'tribune/:tribune/:page/login',
          },
        }, {
          initialRouteParams: {tribune: tribuneObject, page: null}
        });

        class PageTribune extends React.Component {
          static tribune = tribuneObject
          static tribuneId = i

          static navigationOptions = ({navigation, screenProps}) => {
            return { title: tribuneConfiguration.title, }
          }

          render() {
            return (
               <NavigationStack
                 screenProps={{tribune: this.constructor.tribune, tribuneId: i, drawerNavigation: this.props.navigation}}
               />
            );
          }
        }

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

