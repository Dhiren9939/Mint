import api, {
  type FileState,
  type ExpiryDuration,
  type ApiResponse,
} from "./index";

export interface GenerateUploadLinkRequest {
  expiryDuration: ExpiryDuration;
  maxDownload: number;
  fileName: string;
  contentType: string;
  contentSize: number;
}

export interface GenerateUploadLinkResponse {
  fileUrl: string;
  fileCode: string;
  fileKey: string;
  expiresAt: string;
  maxDownloadCount: number;
  fileState: FileState;
}

async function getUploadLink(
  file: File,
  expiryDuration: ExpiryDuration,
  maxDownloadCount: number,
) {
  return api.post<ApiResponse<GenerateUploadLinkResponse>>(
    "/api/v1/file/upload",
    {
      fileName: file.name,
      contentType: "application/octet-stream",
      contentSize: file.size,
      expiryDuration,
      maxDownloadCount,
    },
  );
}

export default getUploadLink;
