const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const appDirectory = path.resolve(__dirname);

module.exports = {
  mode: 'development',
  entry: path.resolve(appDirectory, 'index.js'),
  output: {
    path: path.resolve(appDirectory, 'dist'),
    filename: 'bundle.web.js',
  },
  resolve: {
    extensions: ['.web.js', '.js', '.jsx', '.ts', '.tsx'],
    alias: {
      'react-native$': 'react-native-web',
      'react-native-vector-icons': 'react-native-vector-icons/dist',
    },
    fullySpecified: false,
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx|ts|tsx)$/,
        // FIX 2: 예외 목록에 'react-native-swipe-gestures' 추가
        exclude: /node_modules\/(?!(@react-native-community|react-native-calendars|react-native-reanimated|react-native-gesture-handler|@react-navigation|react-native-swipe-gestures)\/).*/,
        use: {
          loader: 'babel-loader',
        },
      },
      // FIX 1: 이미지 파일을 처리하기 위한 로더 규칙 추가
      {
        test: /\.(png|jpg|jpeg|gif|svg)$/i,
        type: 'asset/resource',
      },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(appDirectory, 'web/index.html'),
    }),
  ],
  devServer: {
    port: 7079,
    hot: true,
    historyApiFallback: true,
    static: {
      directory: path.resolve(appDirectory, 'web'),
    },
    host: '0.0.0.0',
  },
  },
};
