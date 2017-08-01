// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar, DrawerLayoutAndroid, Text } from 'react-native';

import { DrawerNavigator, StackNavigator } from 'react-navigation';

import { styles } from './src/style';
import { MiaoliMenu,
  Settings,
  PageTribuneSettings, PageTribuneBrowser, PageTribuneLogin,
  PageFeedBrowser,
  PageNewTribuneType, PageNewTribunePredefined, PageNewTribuneDetails,
  Tribune } from './src/tribune';

export default class App extends React.Component {
  constructor(props) {
    super(props)

    this.state = {
      configurationLoaded: false,
      configuration: [],
      tribunes: [],
    };

    Settings.app = this
  }

  componentDidMount = () => {
    Settings.loadConfiguration(settings => {
      this.setState({configurationLoaded: true, configuration: settings})
    })
  }

  render() {
    const displayNavigation = this.state.configurationLoaded ? 'flex' : 'none';
    const displayLoading = this.state.configurationLoaded ? 'none' : 'flex';

    console.log('rendering main')

    if (this.state.configurationLoaded) {
      var screens = {}
      const allTribunes = this.state.tribunes
      const NavigationStack = StackNavigator({
        FeedHome: {
          screen: PageFeedBrowser,
          path: 'tribune/:tribunes/:page',
        },
        /*FeedSettings: {
          screen: PageFeedSettings,
          path: 'tribune/:tribunes/:page/settings',
        },*/
      }, {
        initialRouteParams: {tribunes: allTribunes, page: null}
      });

      class PageFeed extends React.Component {
        static tribunes = allTribunes

        static navigationOptions = ({navigation, screenProps}) => {
          return { title: "All tribunes", }
        }

        render() {
          return (
             <NavigationStack
               screenProps={{title: "All tribunes", tribunes: this.constructor.tribunes, drawerNavigation: this.props.navigation}}
             />
          );
        }
      }

      screens["feeds-all"] = {
        screen: PageFeed,
      }

      this.state.configuration.forEach((tribuneConfiguration, i) => {
        var tribuneObject = new Tribune(tribuneConfiguration)

        this.state.tribunes.push(tribuneObject)

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

          static navigationOptions = ({navigation, screenProps}) => {
            return { title: tribuneConfiguration.title, }
          }

          render() {
            return (
               <NavigationStack
                 screenProps={{tribune: this.constructor.tribune, drawerNavigation: this.props.navigation}}
               />
            );
          }
        }

        screens["tribune-" + i] = {
          screen: PageTribune,
        }
      })

      const NavigationStackNew = StackNavigator({
        TribuneType: {
          screen: PageNewTribuneType,
          path: 'tribune/new',
        },
        TribuneChoosePredefined: {
          screen: PageNewTribunePredefined,
          path: 'tribune/new/predefined',
        },
        TribuneDetails: {
          screen: PageNewTribuneDetails,
          path: 'tribune/new/details',
        },
      });

      class PageTribune extends React.Component {
        static tribunes = allTribunes

        static navigationOptions = ({navigation, screenProps}) => {
          return { title: "New tribune", }
        }

        render() {
          return (
             <NavigationStackNew
               screenProps={{title: "New tribune", drawerNavigation: this.props.navigation}}
             />
          );
        }
      }

      screens["feeds-new"] = {
        screen: PageTribune,
      }


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

