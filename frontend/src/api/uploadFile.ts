import axios from "axios";

async function uploadFile(
  file: File,
  uploadURL: string,
  contentType = "application/octet-stream",
) {
  return axios.put(uploadURL, file, {
    headers: {
      "Content-Type": contentType,
    },
  });
}

export default uploadFile;
