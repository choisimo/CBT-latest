// App.tsx
import React from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { AuthProvider } from './src/context/AuthContext';  // ← import
import RootNavigator from './src/navigation/RootNavigator';

export default function App() {
  return (
    // ① AuthProvider를 최상단에 두고
    <AuthProvider>
      <GestureHandlerRootView style={{ flex: 1 }}>
        <RootNavigator />
      </GestureHandlerRootView>
    </AuthProvider>
  );
}
