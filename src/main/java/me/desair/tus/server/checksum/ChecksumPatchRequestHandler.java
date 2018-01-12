package me.desair.tus.server.checksum;

import static me.desair.tus.server.checksum.ChecksumAlgorithm.CHECKSUM_VALUE_SEPARATOR;

import java.io.IOException;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.RequestHandler;
import me.desair.tus.server.checksum.validation.ChecksumAlgorithmValidator;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadChecksumMismatchException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.apache.commons.lang3.StringUtils;

public class ChecksumPatchRequestHandler implements RequestHandler {

    @Override
    public boolean supports(final HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

    @Override
    public void process(final HttpMethod method, final TusServletRequest servletRequest, final TusServletResponse servletResponse,
                        final UploadStorageService uploadStorageService, final String ownerKey) throws IOException, TusException {
        String uploadChecksumHeader = servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM);

        if(servletRequest.hasCalculatedChecksum() && StringUtils.isNotBlank(uploadChecksumHeader)) {

            try {
                //The Upload-Checksum header can be a trailing header which is only present after reading the full content.
                //Therefor we need to revalidate that header here
                new ChecksumAlgorithmValidator().validate(method, servletRequest, uploadStorageService, ownerKey);

                //Everything is valid, check if the checksum matches
                String expectedValue = StringUtils.substringAfter(uploadChecksumHeader, CHECKSUM_VALUE_SEPARATOR);

                ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.forUploadChecksumHeader(uploadChecksumHeader);
                String calculatedValue = servletRequest.getCalculatedChecksum(checksumAlgorithm);

                if (!StringUtils.equals(expectedValue, calculatedValue)) {
                    throw new UploadChecksumMismatchException("Expected checksum " + expectedValue
                            + " but was " + calculatedValue
                            + " with algorithm " + checksumAlgorithm);
                }

            } catch (TusException ex) {
                //There was an error. Remove the bytes we've read and written. Then pass the error.
                UploadInfo uploadInfo = uploadStorageService.getUploadInfo(servletRequest.getRequestURI(), ownerKey);

                uploadStorageService.removeLastNumberOfBytes(uploadInfo, servletRequest.getBytesRead());

                throw ex;
            }
        }
    }
}