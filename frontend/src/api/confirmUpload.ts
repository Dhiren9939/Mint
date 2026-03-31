import api, { type FileState, type ApiResponse } from "./index";

export interface ConfirmUploadLinkRequest {
  fileKey: string;
  fileCode: string;
}

export interface ConfirmUploadLinkResponse {
  fileCode: string;
  expiresAt: string;
  maxDownloadCount: string;
  fileState: FileState;
}

async function confirmUpload(fileKey: string, fileCode: string) {
  return api.post<ApiResponse<ConfirmUploadLinkResponse>>("/api/v1/file", {
    fileKey,
    fileCode,
  });
}

export default confirmUpload;
