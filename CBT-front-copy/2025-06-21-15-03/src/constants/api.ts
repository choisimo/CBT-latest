// src/constants/api.ts
export const BASIC_URL = 'https://auth.nodove.com/swagger-ui/index.html';


// src/api.ts
export const buildCustomEmailPayload = (
  email: string,
  title: string,
  content: string
) => ({
  url: `/api/private/customEmailSend`,
  options: {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // Authorization은 fetchWithAuth에서 붙음
    },
    body: JSON.stringify({ email, title, content }),
  },
});
