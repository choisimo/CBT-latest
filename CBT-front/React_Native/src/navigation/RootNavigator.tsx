// src/navigation/RootNavigator.tsx
import React, { useContext } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import AuthStack from './AuthStack';
import AppStack from './AppStack';
import { AuthContext } from '../context/AuthContext';
import { View, ActivityIndicator } from 'react-native';

export default function RootNavigator() {
  const { isBootstrapping, isAuthLoading, user } = useContext(AuthContext);

  // 앱 기동 시 또는 인증 액션 중 로딩 표시
  if (isBootstrapping || isAuthLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      {/* 로그인 전 또는 이메일 인증 미완료 시 AuthStack, 모두 완료 시 AppStack */}
      {/* {<AppStack />} */}
      {user ? <AppStack />:<AuthStack />}
    </NavigationContainer>
  );
}
