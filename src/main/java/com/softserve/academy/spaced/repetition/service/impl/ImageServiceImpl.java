package com.softserve.academy.spaced.repetition.service.impl;

import com.google.api.client.util.Base64;
import com.softserve.academy.spaced.repetition.domain.Image;
import com.softserve.academy.spaced.repetition.domain.User;
import com.softserve.academy.spaced.repetition.repository.ImageRepository;
import com.softserve.academy.spaced.repetition.service.ImageService;
import com.softserve.academy.spaced.repetition.service.UserService;
import com.softserve.academy.spaced.repetition.utils.exceptions.CanNotBeDeletedException;
import com.softserve.academy.spaced.repetition.utils.exceptions.ImageRepositorySizeQuotaExceededException;
import com.softserve.academy.spaced.repetition.utils.exceptions.NotAuthorisedUserException;
import com.softserve.academy.spaced.repetition.utils.exceptions.NotOwnerOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Service
public class ImageServiceImpl implements ImageService {
    @Autowired
    private UserService userService;
    @Autowired
    private ImageRepository imageRepository;
    @Value("${app.images.maxSize}")
    private Long maxFileSize;
    @Value("${app.images.userQuote}")
    private Long userQuote;

    @Override
    public Image addImageToDB(MultipartFile file)
            throws ImageRepositorySizeQuotaExceededException, NotAuthorisedUserException {
        checkImageExtension(file);
        User user = userService.getAuthorizedUser();
        Image image = new Image(encodeToBase64(file), file.getContentType(), user, file.getSize());
        imageRepository.save(image);
        image = imageRepository.getImageWithoutContentById(image.getId());
        return image;
    }

    @Override
    public void checkImageExtension(MultipartFile file) throws ImageRepositorySizeQuotaExceededException,
            NotAuthorisedUserException {
        long fileSize = file.getSize();
        User user = userService.getAuthorizedUser();
        if (fileSize > getUsersLimitInBytesForImagesLeft(user.getId())) {
            throw new ImageRepositorySizeQuotaExceededException();
        }
        if (fileSize > maxFileSize) {
            throw new MultipartException("File upload error: file is too large.");
        } else {
            String imageType = file.getContentType();
            if (imageType == null || !imageType.split("/")[0].equalsIgnoreCase("image")) {
                throw new IllegalArgumentException("File upload error: file is not an image");
            }
        }
    }

    @Override
    public byte[] getDecodedImageContentByImageId(Long id) {
        byte[] imageContent = null;
        List<Long> idList = imageRepository.getIdList();
        for (Long existingId : idList) {
            if (id.equals(existingId)) {
                Image image = imageRepository.findImageById(id);
                String encodedFileContent = image.getImagebase64();
                imageContent = decodeFromBase64(encodedFileContent);
                break;
            }
        }
        return imageContent;
    }

    @Override
    public String encodeToBase64(MultipartFile file) {
        String encodedFile = null;
        byte[] bytes = new byte[(int) file.getSize()];
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        encodedFile = Base64.encodeBase64String(bytes);
        return encodedFile;
    }

    @Override
    public byte[] decodeFromBase64(String encodedFileContent) {

        return Base64.decodeBase64(encodedFileContent);
    }

    @Override
    public Long getUsersLimitInBytesForImagesLeft(Long userId) {

        Long bytesUsed = imageRepository.getSumOfImagesSizesOfUserById(userId);
        if (bytesUsed == null) {
            bytesUsed = 0L;
        }
        Long bytesLeft = userQuote - bytesUsed;
        return bytesLeft;
    }

    @Override
    public void deleteImage(Long id)
            throws CanNotBeDeletedException, NotOwnerOperationException, NotAuthorisedUserException {
        Image image = imageRepository.findImageById(id);
        Long imageOwnerId = image.getCreatedBy().getId();
        Long userId = 0L;
        userId = userService.getAuthorizedUser().getId();
        if (imageOwnerId != userId) {
            throw new NotOwnerOperationException();
        }
        boolean isUsed = image.getIsImageUsed();
        if (isUsed) {
            throw new CanNotBeDeletedException();
        } else {
            imageRepository.delete(image);
        }
    }

    @Override
    public void setImageStatusInUse(Long imageId) {
        Image image = imageRepository.findOne(imageId);
        image.setIsImageUsed(true);
        imageRepository.save(image);
    }

    @Override
    public void setImageStatusNotInUse(Long imageId) {
        Image image = imageRepository.findOne(imageId);
        image.setIsImageUsed(false);
        imageRepository.save(image);
    }

    @Override
    public List<Image> getImagesForCurrentUser() throws NotAuthorisedUserException {
        Long userId = userService.getAuthorizedUser().getId();
        return imageRepository.getImagesWithoutContentById(userId);
    }
}
