import type { ApiResponse } from "@/types/apiResponse.d.ts";
import axios from "axios";
import type { InternalAxiosRequestConfig, AxiosError } from "axios";
import { updateTokenManually } from "@/features/slices/auth/authSlice";
import type { AppDispatch } from "@/features/store";
import { logout } from "@/features/slices/auth/authThunk";
import { refreshTokenApi } from "@/services/authApi";

// ============================================================
// Setup dispatch từ store để sử dụng
// trong đây
// ============================================================
let dispatchRef: AppDispatch;

export const setupAxiosInterceptors = (dispatch: AppDispatch) => {
  dispatchRef = dispatch;
};

// ============================================================
// Cấu hình mặc định cho các request
// ============================================================
const axiosClient = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
});

// ============================================================
// Interceptor: Tự động gắn Access Token vào mỗi request
// CHO TRƯỜNG HỢP ACCESS TOKEN ĐƯỢC LƯU Ở LOCAL STORAGE
// ============================================================
axiosClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const accessToken = localStorage.getItem("access_token");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// ============================================================
// Cơ chế hàng đợi xử lý request bị lỗi 401 trong khi refresh token:
// - failedQueue lưu các request bị 401
// - Khi refresh thành công → resolve queue
// - Khi refresh fail → reject toàn bộ queue
// ============================================================
type FailedRequest = {
  resolve: () => void;
  reject: (reason?: unknown) => void;
};

let failedQueue: FailedRequest[] = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (token) resolve();
    else reject(error);
  });
  failedQueue = [];
};

// ============================================================
// Interceptor: Xử lý lỗi 401 và errorCode UNAUTHORIZED
// ============================================================

let isRefreshing = false;

axiosClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // Check lỗi có phải thuộc lại lỗi JWT Token
    const { response } = error;
    const errorCode = (response?.data as ApiResponse<null>)?.errorCode;
    const isUnauthorized =
      response?.status === 401 &&
      (errorCode === "UNAUTHORIZED" || errorCode === "ACCESS_DENIED");

    if (isUnauthorized && !originalRequest._retry) {
      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise<void>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => axiosClient(originalRequest)) // trigger khi resolve()
          .catch((err) => Promise.reject(err)); // trigger khi reject()
      }

      isRefreshing = true;

      try {
        const res = (await refreshTokenApi()).data.data;

        const accessToken = res.accessToken;
        dispatchRef(updateTokenManually(res));
        processQueue(null, accessToken);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return axiosClient(originalRequest);
      } catch (refreshError) {
        dispatchRef(logout());
        processQueue(refreshError, null);

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

// ============================================================

export default axiosClient;
