// vim: et ts=2 sts=2 sw=2

import { AsyncStorage } from 'react-native';

export class Settings {
  static app = null
  static settings = []

  static user_agent = "Miaoli/0.2"

  static knownTribunes = {
    moules: {
      id: 'moules',
      title: 'Moules',
      color: "#fde096",
      backend: 'http://moules.org/board/last.php?backend=tsv',
      backend_type: 'tsv',
      post_url: 'http://moules.org/board/add.php',
      post_format: 'message=%s',
    },
    dlfp: {
      id: 'dlfp',
      title: 'DLFP',
      color: "#67c6f2",
      backend: 'https://linuxfr.org/board/index.xml',
      backend_type: 'xml-htmlentitised',
      post_url: 'https://linuxfr.org/board',
      post_format: 'board[message]=%s',
      loginpage: 'https://linuxfr.org',
    },
    euromussels: {
      id: 'euromussels',
      title: 'Euromussels',
      color: "#f4b189",
      backend: 'http://faab.euromussels.eu/data/backend.xml',
      backend_type: 'xml-raw',
      post_url: 'http://faab.euromussels.eu/add.php',
      post_format: 'message=%s',
      loginpage: 'http://faab.euromussels.eu/loginF.php',
    },
    adonai: {
      id: 'adonai',
      title: 'Adonai',
      color: "#4bd0e3",
      backend: 'http://miaoli.im/tribune/papitalisme/tsv',
      backend_type: 'tsv',
      post_url: 'http://miaoli.im/tribune/papitalisme/post',
      post_format: 'message=%s',
    },
    batavie: {
      id: 'batavie',
      title: 'Batavie',
      color: "#4bd0e3",
      backend: 'http://batavie.leguyader.eu/remote.xml',
      backend_type: 'xml-raw',
      post_url: 'http://batavie.leguyader.eu/index.php/add',
      post_format: 'message=%s',
    },
    sveetch: {
      id: 'sveetch',
      title: 'Sveetch',
      color: "#4bd0e3",
      backend: 'http://sveetch.net/tribune/crap/remote.xml',
      backend_type: 'xml-raw',
      post_url: 'http://sveetch.net/tribune/crap/post.xml',
      post_format: 'message=%s',
      loginpage: 'http://sveetch.net/tribune',
    },
    ratatouille: {
      id: 'ratatouille',
      title: 'Ratatouille',
      color: "#4bd0e3",
      backend: 'http://ratatouille.leguyader.eu:80/data/backend.xml',
      backend_type: 'xml-raw',
      post_url: 'http://ratatouille.leguyader.eu:80/add.php',
      post_format: 'message=%s',
      loginpage: 'http://ratatouille.leguyader.eu',
    },
    gabuzomeu: {
      id: 'gabuzomeu',
      title: 'GaBuZoMeu',
      color: "#4bd0e3",
      backend: 'https://jb3.plop.cc/legacy/xml',
      backend_type: 'xml-htmlentitised',
      post_url: 'https://jb3.plop.cc/legacy/post',
      post_format: 'message=%s'
    },
  }

  static defaultSettings = [
    {
      title: 'Moules',
      color: "#fde096",
      backend: 'http://moules.org/board/last.php?backend=tsv',
      backend_type: 'tsv',
      post_url: 'http://moules.org/board/add.php',
      post_format: 'message=%s',
      user_agent: Settings.user_agent,
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
      user_agent: Settings.user_agent,
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
      user_agent: Settings.user_agent,
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
      user_agent: Settings.user_agent,
      cookie: '',
    },
  ]

  static loadConfiguration = (callback) => {
    AsyncStorage
      .getItem("tribune:configuration")
      .then((result) => {

        if (!result) {
          AsyncStorage.setItem('tribune:configuration', JSON.stringify(Settings.defaultSettings))
          return Settings.defaultSettings
        } else {
          var settings = JSON.parse(result);

          console.log(settings)
          for (var i in settings) {
            var backend = settings[i].backend

            for (var j in Settings.defaultSettings) {
              if (Settings.defaultSettings[j].backend == backend) {
                for (var key in Settings.defaultSettings[j]) {
                  if (settings[i][key] === undefined) {
                    settings[i][key] = Settings.defaultSettings[j][key]
                    console.log([backend, key, Settings.defaultSettings[j][key]])
                  } else if (key == "color" && settings[i][key] === "blue") {
                    settings[i][key] = Settings.defaultSettings[j][key]
                    console.log([backend, key, Settings.defaultSettings[j][key]])
                  }
                }
              }
            }
          }

          Settings.settings = settings

          if (callback) {
            callback(settings)
          }
        }
      })
  }

  static saveTribuneSetting = (savedTribune, key, value) => {
    AsyncStorage
      .getItem("tribune:configuration")
      .then((result) => {
        if (result) {
          Settings.settings.forEach((existingTribune) => {
            if (existingTribune.backend == savedTribune.backend) {
              existingTribune[key] = value
            }
          })

          Settings.saveSettings()
        }
      })
  }

  static saveSettings = () => {
    AsyncStorage.setItem('tribune:configuration', JSON.stringify(Settings.settings))
  }

  static activateTribune = (tribune) => {
    Settings.settings.push(tribune)

    if (!tribune.user_agent) {
      tribune.user_agent = Settings.user_agent
    }

    Settings.app.setState({configuration: Settings.settings})
    Settings.saveSettings()
    console.log(['settings', Settings.settings])
  }
}

