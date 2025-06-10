import React, { useContext } from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AuthContext } from '../context/AuthContext';
import LoginScreen from '../screens/auth/SignInScreen';
import SignupScreen from '../screens/auth/SignupScreen';
import EmailVerificationScreen from '../screens/auth/EmailVerificationScreen';

export type AuthStackParamList = {
  SignIn: undefined;
  SignUp: undefined;
  VerifyEmail: undefined;
};

const Stack = createNativeStackNavigator<AuthStackParamList>();

export default function AuthStack() {
  const { user } = useContext(AuthContext);
  // user가 없거나 이메일 인증되지 않은 경우 VerifyEmail을 초기 화면으로
  const initialRouteName: keyof AuthStackParamList =
    user && !user.emailVerified ? 'VerifyEmail' : 'SignIn';

  return (
    <Stack.Navigator
      initialRouteName={initialRouteName}
      screenOptions={{ headerShown: false }}
    >
      <Stack.Screen name="SignIn" component={LoginScreen} />
      <Stack.Screen name="SignUp" component={SignupScreen} />
      <Stack.Screen
        name="VerifyEmail"
        component={EmailVerificationScreen}
        options={{ title: '이메일 인증' }}
      />
    </Stack.Navigator>
  );
}
