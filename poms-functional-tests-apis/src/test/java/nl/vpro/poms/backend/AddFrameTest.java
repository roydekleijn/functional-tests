package nl.vpro.poms.backend;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.*;

import nl.vpro.domain.image.ImageType;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.update.ImageUpdate;
import nl.vpro.domain.media.update.ProgramUpdate;
import nl.vpro.poms.AbstractApiMediaBackendTest;

import static nl.vpro.testutils.Utils.waitUntil;

/**
 * 2018-08-14
 * 5.9-SNAPSHOT @ dev :ok
 * 2019-05-06
 * 5.11-SNAPSHOT @ dev :ok
 *
 * @ test: 403 permission denied (we moeten hiervoor een account hebben, anders kunnen we niet testen!)
 * 5.7.9 @ test: 403 permission denied (we moeten hiervoor een account hebben, anders kunnen we niet testen!)
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
@Log4j2
public class AddFrameTest extends AbstractApiMediaBackendTest {

    private static final Duration ACCEPTABLE_DURATION = Duration.ofMinutes(3);

    private static final Duration OFFSET = Duration.ofMinutes(10).plus(Duration.ofMinutes((int) (20f * Math.random())));

    private static final long ORIGINAL_SIZE_OF_IMAGE = 2621; // This is the size of an image we upload in test03

    private static String createImageUri;

    @BeforeAll
    static void init() {
        log.info("Offset for this test {}", OFFSET);
    }


    @Test
    public void test01AddFrame() {
        Program fullProgram = backend.getFullProgram(MID);
        if (fullProgram.getImage(ImageType.PICTURE) == null) {
            log.info("No image with type PICTURE yet present");
            log.info(backend.addImage(randomImage(title).build(), MID));
        }
        try (Response response = backend.getFrameCreatorRestService().createFrame(
            MID, OFFSET, null, null, getClass().getResourceAsStream("/VPRO.png"))) {
            log.info("Response: {}", response);
        }

    }

    @Test
    public void test02CheckArrived() {
        final ProgramUpdate[] update = new ProgramUpdate[1];

        waitUntil(ACCEPTABLE_DURATION,
            MID + " has image STILL with offset " + OFFSET + " and size != " + ORIGINAL_SIZE_OF_IMAGE,
            () -> {
                update[0] = backend_authority.get(MID);
                if (update[0] == null) {
                    return false;
                }
                ImageUpdate foundImage =
                    update[0].getImages()
                        .stream()
                        .filter(iu ->
                            iu != null &&
                                iu.getOffset() != null &&
                                iu.getOffset().equals(OFFSET) &&
                                iu.getType() == ImageType.STILL
                        ).findFirst().orElse(null);
                if (foundImage == null) {
                    log.info("No STILL found yet at {}", OFFSET);
                    return false;
                }

                long foundSize = imageUtil.getSize(foundImage).orElse(-1L);
                if (foundSize == ORIGINAL_SIZE_OF_IMAGE) {
                    log.info("Found {} but the size is the original size, so this may be from test10", foundImage);
                    return false;
                }
                createImageUri = foundImage.getImageUri();
                return true;
            });
    }



    @Test
    public void test10Overwrite() {

        try (Response response = backend.getFrameCreatorRestService().createFrame(MID, OFFSET, null, null, getClass().getResourceAsStream("/VPRO1970's.png"))) {
            log.info("{}", response);
        }
        waitUntil(ACCEPTABLE_DURATION,
            MID + " has STILL image with offset " + OFFSET + " and size " + ORIGINAL_SIZE_OF_IMAGE,
            () -> {
                ProgramUpdate p  = backend_authority.get(MID);
                if (p == null) {
                    throw new IllegalStateException("Program " + MID + " not found");
                }

                ImageUpdate foundImage = p.getImages()
                        .stream()
                        .filter(iu ->
                            iu != null &&
                                iu.getOffset() != null &&
                                iu.getOffset().equals(OFFSET) &&
                                iu.getType() == ImageType.STILL)
                        .findFirst()
                    .orElse(null)
                    ;

                if (foundImage == null) {
                    //return false;
                    throw new IllegalStateException("No image found for " + MID + " with offset " + OFFSET);
                }
                String uri = foundImage.getImageUri();
                if (uri.equals(createImageUri)) {
                    return false;
                }
                long newSize = imageUtil.getSize(foundImage).orElse(-1L);
                if (newSize == ORIGINAL_SIZE_OF_IMAGE || newSize == -1L) {
                    return true;
                }
                return false;

            });
    }


    @Test
    public void test98Cleanup() {
        ProgramUpdate update = backend_authority.get(MID);
        Assumptions.assumeTrue(update != null);
        log.info("Removing images " + update.getImages());
        update.getImages().clear();
        log.info("{}", backend_authority.set(update));

    }


    @Test
    public void test99CheckCleanup() {
         waitUntil(ACCEPTABLE_DURATION,
            MID + " has no stills",
            () -> {
                try {
                    log.info("Getting full {}", MID);
                    ;
                    Program p = backend.getFullProgram(MID);
                    log.info("Found images for {}: {}", MID, p.getImages());
                    return
                        p.getImages()
                            .stream()
                            .noneMatch(iu -> iu != null && iu.getOwner() == OwnerType.AUTHORITY && iu.getType() == ImageType.STILL);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return false;
                }
            });


    }

}
