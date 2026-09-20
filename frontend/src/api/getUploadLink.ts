import api, {
  type FileState,
  type ExpiryDuration,
  type ApiResponse,
} from "./index";

export interface GenerateUploadLinkRequest {
  expiryDuration: ExpiryDuration;
  fileName: string;
  contentType: string;
  contentSize: number;
}

export interface GenerateUploadLinkResponse {
  fileUrl: string;
  fileCode: string;
  fileKey: string;
  expiresAt: string;
  fileState: FileState;
}

async function getUploadLink(file: File, expiryDuration: ExpiryDuration) {
  return api.post<ApiResponse<GenerateUploadLinkResponse>>(
    "/api/v1/file/upload",
    {
      fileName: file.name,
      contentType: "application/octet-stream",
      contentSize: file.size,
      expiryDuration,
    },
  );
}

export default getUploadLink;
