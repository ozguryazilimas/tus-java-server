package me.desair.tus.server.core.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.Utils;
import me.desair.tus.server.exception.InvalidContentLengthException;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadIdFactory;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class ContentLengthValidator extends AbstractRequestValidator {
    @Override
    public void validate(final HttpMethod method, final HttpServletRequest request, final UploadStorageService uploadStorageService, final UploadIdFactory idFactory) throws TusException {
        Long contentLength = Utils.getLongHeader(request, HttpHeader.CONTENT_LENGTH);

        UUID id = idFactory.readUploadId(request);
        UploadInfo uploadInfo = uploadStorageService.getUploadInfo(id);

        if(contentLength != null
                && uploadInfo != null
                && uploadInfo.hasLength()
                && (uploadInfo.getOffset() + contentLength > uploadInfo.getLength())) {

            throw new InvalidContentLengthException("The " + HttpHeader.CONTENT_LENGTH + " value " + contentLength
                    + " in combination with the current offset " + uploadInfo.getOffset() + " exceeds the declared upload length "
                    + uploadInfo.getLength());
        }
    }

    @Override
    public boolean supports(final HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

}