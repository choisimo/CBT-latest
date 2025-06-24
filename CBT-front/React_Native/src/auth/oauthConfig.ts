// src/auth/oauthConfig.ts
import { AuthorizationServiceConfiguration } from 'react-native-app-auth';
import Config from 'react-native-config';

export const NAVER_CONFIG = {
  clientId: Config.NAVER_CLIENT_ID,
  clientSecret: Config.NAVER_CLIENT_SECRET,
  redirectUrl: Config.REDIRECT_URI,
  scopes: ['profile'],
  serviceConfiguration: {
    authorizationEndpoint: 'https://nid.naver.com/oauth2.0/authorize',
    tokenEndpoint:         'https://nid.naver.com/oauth2.0/token',
  },
};

export const KAKAO_CONFIG = {
  clientId: Config.KAKAO_CLIENT_ID,
  redirectUrl: Config.REDIRECT_URI,
  scopes: ['account_email', 'profile_nickname'],
  serviceConfiguration: {
    authorizationEndpoint: 'https://kauth.kakao.com/oauth/authorize',
    tokenEndpoint:         'https://kauth.kakao.com/oauth/token',
  },
};
