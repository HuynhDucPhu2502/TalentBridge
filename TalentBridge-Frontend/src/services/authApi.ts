import axiosClient from "@/lib/axiosClient";
import type { ApiResponse } from "@/types/apiResponse.types.ts";
import type {
  loginRequestDto,
  AuthTokenResponseDto,
  UserSessionResponseDto,
  UserDetailsResponseDto,
  SessionMetaRequest,
  SessionMetaResponse,
} from "@/types/user.types.ts";
import { getSessionMeta } from "@/utils/sessionHelper";
import axios from "axios";

export const loginApi = (data: loginRequestDto) => {
  data = {
    ...data,
    sessionMetaRequest: getSessionMeta(),
  };

  return axiosClient.post<ApiResponse<AuthTokenResponseDto>>(
    "/auth/login",
    data,
  );
};

export const logoutApi = () => {
  return axios.post(
    "http://localhost:8080/auth/logout",
    {},
    { withCredentials: true },
  );
};

export const getUserSession = () => {
  return axiosClient.get<ApiResponse<UserSessionResponseDto>>("/auth/me");
};

export const getUserDetails = () => {
  return axiosClient.get<ApiResponse<UserDetailsResponseDto>>(
    "/auth/me/details",
  );
};

export const refreshTokenApi = () => {
  const data: SessionMetaRequest = getSessionMeta();

  return axios.post<ApiResponse<AuthTokenResponseDto>>(
    "http://localhost:8080/auth/refresh-token",
    data,
    { withCredentials: true },
  );
};

export const getSessions = () => {
  return axiosClient.get<ApiResponse<SessionMetaResponse[]>>(`/auth/sessions`);
};
