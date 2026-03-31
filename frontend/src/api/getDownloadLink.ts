import api, { type ApiResponse } from "./index";

export interface GetDownloadLinkResponse {
  fileUrl: string;
  expiresAt: string;
  dowloadCount: number;
  maxDownloadCount: number;
}

async function getDownloadLink(fileCode: string) {
  return api.get<ApiResponse<GetDownloadLinkResponse>>(
    `/api/v1/file/${fileCode}`,
  );
}

export default getDownloadLink;
