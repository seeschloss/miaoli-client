// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { FlatList, ScrollView } from 'react-native';

import { styles } from './style';

export class TribunePosts extends React.Component {
  constructor(props) {
    super(props);

    this.refreshing = false;
  }

  setRefreshing = (refreshing) => {
    this.refreshing = refreshing;

    if (this.flatList) {
      this.flatList.refreshing = refreshing;
    }

    if (!refreshing) {
      this.scrollToBottom();
    }
  }

  // when provided data array changes,
  // update the internal reversed copy
  componentWillReceiveProps({ data }) {
    if (data !== this._previousData) {
      this._previousData = data;
      this.setState({ data: [...data].reverse() });
    }
  }

  scrollToBottom() {
    this.scrollToIndex({ index: 0 });
  }

  scrollToIndex(...args) {
    if (this._listViewRef) {
      this._listViewRef.scrollToIndex(...args);
    }
  }

  renderScrollComponent = ({ style, refreshing, ...props }) => (
    <ScrollView keyboardShouldPersistTaps={'always'} style={[style, styles.flip]} />
  );

  renderItem = props => (
    props.item.post
  );

  onRefresh = () => {
    this.props.tribune.update()
  }

  render() {
    return (
      <FlatList
        contentContainerStyle={styles.tribunePosts}
        onRefresh={this.onRefresh}
        ref={(ref) => { this.flatList = ref }}
        extraData={this.props.tribune.state.posts}
        data={this.props.tribune.postsList().reverse()}
        renderItem={this.renderItem}
        renderScrollComponent={this.renderScrollComponent}
        />
    );
  }
}


