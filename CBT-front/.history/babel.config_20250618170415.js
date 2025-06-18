module.exports = {
  presets: ['module:@react-native/babel-preset'],
  plugins: [
    'react-native-reanimated/plugin', // 👈 반드시 배열 맨 마지막에!
  ],
  overrides: [
    {
      test: /\.web\.(js|ts|tsx)$/,
      presets: [
        ['@babel/preset-env', { targets: { browsers: ['last 2 versions'] } }],
        '@babel/preset-react',
        '@babel/preset-typescript',
      ],
    },
  ],
};
