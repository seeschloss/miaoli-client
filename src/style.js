// vim: et ts=2 sts=2 sw=2

import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  tribuneContainer: {
    flex: 1,
    backgroundColor: 'black',
  },
  tribune: {
    backgroundColor: 'white',
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'space-around',
    alignItems: 'stretch',
    padding: 5,
  },
  tribuneTitle: {
    flex: 0,
  },
  tribunePosts: {
    flex: 1,
    justifyContent: 'flex-start',
  },
  tribunePost: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'stretch',
    marginTop: 4,
    borderRadius: 10,
    backgroundColor: '#CDDC39',
    borderColor: '#CDDC39',
    borderWidth: 1,
  },
  tribunePostInfoWrapper: {
    flex: 0,
    flexDirection: 'column',
    alignItems: 'stretch',
    justifyContent: 'center',
    borderTopLeftRadius: 10,
    borderBottomLeftRadius: 10,
    borderWidth: 0,
  },
  tribunePostInfo: {
    flex: 1,
    flexDirection: 'column',
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
    backgroundColor: 'white',
    paddingTop: 3,
    paddingBottom: 3,
    paddingLeft: 3,
    paddingRight: 3,
    borderTopLeftRadius: 10,
    borderBottomLeftRadius: 10,
    borderWidth: 0,
  },
  tribunePostClock: {
    flex: 0,
    fontWeight: 'bold',
  },
  tribunePostAuthor: {
    flex: 0,
    color: 'green',
    width: 50,
    overflow: 'hidden',
  },
  tribunePostMessageWrapper: {
    flex: 1,
  },
  tribunePostMessage: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'flex-start',
    alignItems: 'center',
    paddingRight: 5,
    paddingLeft: 5,
    borderTopRightRadius: 10,
    borderBottomRightRadius: 10,
  },
  tribunePostMessageSegment: {
    color: 'black',
  },
  tribunePostMessageSegmentURL: {
    fontWeight: 'bold',
    color: '#1976D2',
    textDecorationLine: 'underline',
    textShadowColor: 'white',
    textShadowOffset: {width: -1, height: -1},
    textShadowRadius: 1,
  },
  tribunePostMessageSegmentClock: {
    color: '#1976D2',
  },
  tribunePostMessageSegmentBold: {
    fontWeight: 'bold',
  },
  tribunePostMessageSegmentItalic: {
    fontStyle: 'italic',
  },
  tribunePostMessageSegmentStrikethrough: {
    textDecorationLine: 'line-through',
  },
  tribunePostMessageSegmentUnderline: {
    textDecorationLine: 'underline',
  },
  tribuneInput: {
    flex: 0,
    flexDirection: 'row',
    justifyContent: 'space-around'
  },
  tribuneInputText: {
    flex: 1,
  },
  tribuneInputButton: {
    width: 200,
    height: 200,
  },
  newPostButtonWrapper: {
    position: 'absolute',
    bottom: 20,
    right: 20,
    width: 40,
    height: 40,
    borderRadius: 40,
  },
  newPostButton: {
    width: 40,
    height: 40,
    borderRadius: 40,
    backgroundColor: '#1976D2',
    alignContent: 'center',
    justifyContent: 'center',
  },
  newPostButtonText: {
    fontSize: 25,
    textAlign: 'center',
  },
  menuItem: {
    padding: 15,
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  menuItemLabel: {
    fontSize: 20,
    color: 'black',
  },
  menuItemValue: {
    fontSize: 15,
    marginLeft: 20,
  },
  menuHeader: {
    padding: 10,
  },
  menuModal: {
    margin: 50,
  },
  flip: {
    transform: [{ scaleY: -1 }],
  },
});

