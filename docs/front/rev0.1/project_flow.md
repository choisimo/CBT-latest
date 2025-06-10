# Application Flow and Navigation

This document outlines the overall application flow, focusing on navigation, authentication, and the structure of different screen stacks.

## 1. Entry Point: `App.tsx`

The application's entry point is `App.tsx`. Its primary responsibilities are:

-   Wrapping the entire application with `AuthProvider`: This component, likely from `src/context/AuthContext.tsx`, manages user authentication state (e.g., user object, loading states, login/logout functions) and makes it available to all components within the application via context.
-   Setting up `GestureHandlerRootView`: This is necessary for handling gestures throughout the application, typically used by navigation libraries.
-   Rendering the `RootNavigator`: This component is responsible for managing the top-level navigation logic.

```typescript
// App.tsx
import React from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { AuthProvider } from './src/context/AuthContext';
import RootNavigator from './src/navigation/RootNavigator';

export default function App() {
  return (
    <AuthProvider>
      <GestureHandlerRootView style={{ flex: 1 }}>
        <RootNavigator />
      </GestureHandlerRootView>
    </AuthProvider>
  );
}
```

## 2. Root Navigator: `RootNavigator.tsx`

`RootNavigator.tsx` acts as the central hub for navigation. It determines which set of screens (or navigation stack) to display to the user.

-   **Loading State**: It checks `isBootstrapping` and `isAuthLoading` from `AuthContext`. If either is true (e.g., when the app is starting up or an authentication process is in progress), it displays a global `ActivityIndicator`.
-   **Stack Selection**:
    -   It uses `NavigationContainer` to enable navigation.
    -   Based on the `user` object and `user.emailVerified` status from `AuthContext`, it decides whether to show the `AuthStack` (for unauthenticated or unverified users) or the `AppStack` (for authenticated and verified users).
    -   **Note**: The current implementation in the provided code has the logic for switching stacks commented out (`{/* {!user || !user.emailVerified ? <AuthStack /> : <AppStack />} */}`) and defaults to showing `<AppStack />`. For the purpose of this documentation, we will assume the intended logic is to switch between these stacks.

```typescript
// src/navigation/RootNavigator.tsx
import React, { useContext } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import AuthStack from './AuthStack';
import AppStack from './AppStack';
import { AuthContext } from '../context/AuthContext';
import { View, ActivityIndicator } from 'react-native';

export default function RootNavigator() {
  const { isBootstrapping, isAuthLoading, user } = useContext(AuthContext);

  if (isBootstrapping || isAuthLoading) {
    // ... loading indicator ...
  }

  return (
    <NavigationContainer>
      {/* Intended logic: */}
      {/* {!user || !user.emailVerified ? <AuthStack /> : <AppStack />} */}
      {/* Current implementation: */}
      {<AppStack />}
    </NavigationContainer>
  );
}
```

## 3. Authentication Flow: `AuthStack.tsx`

`AuthStack.tsx` manages the screens related to user authentication. It uses a `createNativeStackNavigator` from `@react-navigation/native-stack`. All screens in this stack have `headerShown: false`.

-   **Screens**:
    -   `SignInScreen` (Component: `LoginScreen`):
        -   Purpose: Allows existing users to sign in.
    -   `SignUpScreen` (Component: `SignupScreen`):
        -   Purpose: Allows new users to create an account.
    -   `VerifyEmailScreen` (Component: `EmailVerificationScreen`):
        -   Purpose: Prompts users to verify their email address after signing up or if their email is not yet verified.
        -   Options: Has a title '이메일 인증' (Email Verification).

-   **Initial Route**:
    -   The `initialRouteName` for `AuthStack` is dynamically determined. If a `user` object exists but `user.emailVerified` is `false`, the stack starts at `VerifyEmail`. Otherwise, it defaults to `SignIn`.

```typescript
// src/navigation/AuthStack.tsx
// ... imports ...
export type AuthStackParamList = {
  SignIn: undefined;
  SignUp: undefined;
  VerifyEmail: undefined;
};

const Stack = createNativeStackNavigator<AuthStackParamList>();

export default function AuthStack() {
  const { user } = useContext(AuthContext);
  const initialRouteName: keyof AuthStackParamList =
    user && !user.emailVerified ? 'VerifyEmail' : 'SignIn';

  return (
    <Stack.Navigator
      initialRouteName={initialRouteName}
      screenOptions={{ headerShown: false }}
    >
      <Stack.Screen name="SignIn" component={LoginScreen} />
      <Stack.Screen name="SignUp" component={SignupScreen} />
      <Stack.Screen name="VerifyEmail" component={EmailVerificationScreen} /* ... */ />
    </Stack.Navigator>
  );
}
```

## 4. Main Application Flow: `AppStack.tsx`

`AppStack.tsx` defines the navigation for the main part of the application, accessible after a user is successfully authenticated and their email is verified. It also uses a `createNativeStackNavigator`. All screens in this stack have `headerShown: false`.

-   **Screens**:
    -   `MainScreen` (Component: `MainScreen`):
        -   Purpose: The primary landing screen after login, likely displaying a list of posts or main functionalities.
    -   `WriteScreen` (Component: `WriteScreen`):
        -   Purpose: Allows users to create a new post or edit an existing one.
        -   Params: Can optionally receive a `postId` (e.g., `Write: { postId: string } | undefined`). If `postId` is provided, it's likely for editing an existing post.
    -   `ViewScreen` (Component: `ViewScreen`):
        -   Purpose: Allows users to view the details of a specific post.
        -   Params: Requires a `postId` (e.g., `View: { postId: string }`).
    -   `AnalyzeScreen` (Component: `AnalyzeScreen`):
        -   Purpose: Allows users to view an analysis of a specific post.
        -   Params: Requires a `postId` (e.g., `Analyze: { postId: string }`).

```typescript
// src/navigation/AppStack.tsx
// ... imports ...
export type AppStackParamList = {
  Main: undefined;
  Write: { postId: string } | undefined;
  Analyze: { postId: string };
  View: { postId: string };
};

const Stack = createNativeStackNavigator<AppStackParamList>();

export default function AppStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Main" component={MainScreen} />
      <Stack.Screen name="Write" component={WriteScreen} />
      <Stack.Screen name="View" component={ViewScreen} />
      <Stack.Screen name="Analyze" component={AnalyzeScreen} />
    </Stack.Navigator>
  );
}
```

## 5. Moving Between Authentication and Main Application

The transition between the `AuthStack` and `AppStack` is managed by the `RootNavigator`.

-   **From `AuthStack` to `AppStack`**:
    1.  A user successfully signs in (`SignInScreen`) or signs up (`SignUpScreen`).
    2.  If email verification is required, they are directed to `VerifyEmailScreen`.
    3.  Upon successful login and email verification, the `AuthContext` updates the `user` state (including `user.emailVerified` to `true`).
    4.  The `RootNavigator` detects this change in the `AuthContext`. The condition `!user || !user.emailVerified` becomes `false`.
    5.  The `RootNavigator` then unmounts `AuthStack` and mounts `AppStack`.

-   **From `AppStack` to `AuthStack`**:
    1.  A user signs out from within the `AppStack`.
    2.  The sign-out action, managed by `AuthContext`, would clear the `user` state (set it to `null` or `undefined`).
    3.  The `RootNavigator` detects this change. The condition `!user || !user.emailVerified` becomes `true`.
    4.  The `RootNavigator` then unmounts `AppStack` and mounts `AuthStack`, typically navigating to the `SignInScreen`.

This flow ensures that users only access the main application content after proper authentication and verification, and are redirected to authentication screens if their session is invalid or they log out.
(As noted earlier, the direct switch in `RootNavigator.tsx` is currently commented out, defaulting to `AppStack`. The above describes the typical intended behavior based on the presence of both stacks and `AuthContext`.)
