// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text, Picker } from 'react-native';

import { Icon } from 'react-native-elements';

import { styles } from './style';

export class TribuneInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      text: "",
    }

    if (this.props.tribunes) {
      this.state.currentTribune = this.props.tribunes[0]
    }
  }

  postMessage = () => {
    this.props.onPostMessage(this.state.text, this.state.currentTribune)
  }

  onTextChange = (text) => {
    this.setState({text: text})
  }

  clear = () => {
    this.input.clear();
    this.setState({text: ""})
  }

  append = (text) => {
    this.setState({text: this.state.text + text})
    this.input.focus();
  }

  setActiveTribune = (title) => {
    this.setState({currentTribune: this.props.tribunes.filter(tribune => tribune.configuration.title == title)[0]})
  }

  render() {
    var tribuneItems = []
    var picker = undefined

    if (this.props.tribunes) {
      tribuneItems = this.props.tribunes.map(tribune =>
          <Picker.Item color={tribune.configuration.color}
            key={tribune.configuration.title}
            label={tribune.configuration.title}
            value={tribune.configuration.title} />
      )
      picker = <View style={{width: 50, height: 50}}>
          <Picker
            prompt={"Tribune"}
            style={{width: 50, backgroundColor: 'white'}}
            selectedValue={this.state.currentTribune.configuration.title}
            onValueChange={this.setActiveTribune} >
              {tribuneItems}
          </Picker>
          <View pointerEvents="none" style={{width: 50, height: 50, justifyContent: 'center', alignItems: 'center', position: 'absolute'}}>
            <Icon color={this.state.currentTribune.configuration.color} name="pages" pointerEvents="none" />
          </View>
        </View>
    }

    return (
      <View style={[styles.tribuneInput]}>
        {picker}
        <TextInput ref={(ref) => { this.input = ref }}
            autoCapitalize={'sentences'}
            style={[styles.tribuneInputText]}
            onSubmitEditing={this.postMessage}
            placeholder='Your message'
            onChangeText={text => this.onTextChange(text)}
            value={this.state.text}
            blurOnSubmit={true}
            returnKeyType={'send'}
            />
      </View>
    );
  }
}

