// declarations.d.ts

// react-native-config 모듈 선언
declare module 'react-native-config' {
  interface Env {
    NAVER_CLIENT_ID: string;
    NAVER_CLIENT_SECRET: string;
    KAKAO_CLIENT_ID: string;
    REDIRECT_URI: string;
  }
  const Config: Env;
  export default Config;
}

// react-native-app-auth 모듈 선언
declare module 'react-native-app-auth' {
  export interface AuthorizationServiceConfiguration {
    authorizationEndpoint: string;
    tokenEndpoint: string;
  }

  export interface AuthConfiguration {
    clientId: string;
    clientSecret?: string;
    redirectUrl: string;
    scopes: string[];
    serviceConfiguration: AuthorizationServiceConfiguration;
    additionalParameters?: { [key: string]: string };
  }

  export interface AuthorizeResult {
    accessToken: string;
    accessTokenExpirationDate: string;
    refreshToken?: string;
    idToken?: string;
    tokenType?: string;
  }

  export function authorize(config: AuthConfiguration): Promise<AuthorizeResult>;
}
