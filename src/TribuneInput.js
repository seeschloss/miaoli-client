// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text } from 'react-native';

import { styles } from './style';

export class TribuneInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      text: "",
    }
  }
  postMessage = () => {
    this.props.tribune.post(this.state.text)
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

  active = () => {
    if (this.state.text != "") {
      return true
    }

    if (!this.input) {
      return false
    }

    if (this.input.isFocused()) {
      return true
    }

    return false
  }

  render() {
    if (this.active()) {
      var display = 'flex';
      if (this.postButton) {
        this.postButton.setState({display: 'none'});
      }
    } else {
      var display = 'none';
      if (this.postButton) {
        this.postButton.setState({display: 'none'});
      }
    }

    return (
      <View style={[styles.tribuneInput]}>
        <TextInput ref={(ref) => { this.input = ref }}
            autoCapitalize={'sentences'}
            style={[styles.tribuneInputText, {display: display}]}
            onSubmitEditing={this.postMessage}
            placeholder='Your message'
            onChangeText={text => this.onTextChange(text)}
            value={this.state.text}
            blurOnSubmit={true}
            returnKeyType={'send'}
            />

        <PostButton ref={(ref) => { this.postButton = ref; }} input={this} />
      </View>
    );
  }
}

class PostButton extends React.Component {
  constructor(props) {
    super(props)

    // Ouch, the button hides actual messages, so... let's just not display it for now
    this.state = {
      display: "none",
    }
  }

  showInput = () => {
    this.props.input.append(' ')
    this.setState({display: "none"})
  }

  render() {
    return (
      <TouchableHighlight style={[styles.newPostButtonWrapper, {display: this.state.display}]} onPress={this.showInput}>
        <View style={styles.newPostButton}>
          <Text style={styles.newPostButtonText}>✉️</Text>
        </View>
      </TouchableHighlight>
    );
  }
}

