package nl.vpro.poms.integration;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.*;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import nl.vpro.api.client.utils.MediaRestClientUtils;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.update.ProgramUpdate;
import nl.vpro.domain.subtitles.*;
import nl.vpro.poms.AbstractApiMediaBackendTest;
import nl.vpro.util.Version;

import static java.time.Duration.ZERO;
import static java.util.Locale.CHINESE;
import static java.util.Locale.JAPANESE;
import static nl.vpro.domain.subtitles.SubtitlesType.TRANSLATION;
import static nl.vpro.testutils.Utils.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Michiel Meeuwissen
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class SubtitlesITest extends AbstractApiMediaBackendTest {

    private static final Duration ACCEPTABLE_DURATION_BACKEND = Duration.ofMinutes(2);

    private static final Duration ACCEPTABLE_DURATION_FRONTEND = Duration.ofMinutes(15);

    private static final AvailableSubtitles JAPANESE_TRANSLATION = AvailableSubtitles.published(JAPANESE, TRANSLATION);
    private static final AvailableSubtitles CHINESE_TRANSLATION = AvailableSubtitles.published(CHINESE, TRANSLATION);

    private static String firstTitle;

    private static boolean arrivedInBackend = false;

    @Test
    @Order(1)
    void addSubtitles() {
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 1));
        assumeThat(backend.getFullProgram(MID_WITH_LOCATIONS).getLocations()).isNotEmpty();

        firstTitle = title;

        String exampleContent = "WEBVTT\n" +
                "\n" +
                "1\n" +
                "00:00:02.200 --> 00:00:04.150\n" +
                "" + title + "\n" +
                "\n" +
                "2\n" +
                "00:00:04.200 --> 00:00:08.060\n" +
                "*'k Heb een paar puntjes die ik met je wil bespreken\n" +
                "\n" +
                "3\n" +
                "00:00:08.110 --> 00:00:11.060\n" +
                "*Dat wil ik doen in jouw mobiele bakkerij\n" +
                "\n" +
                "";
        {
            Subtitles subtitles = Subtitles.webvttTranslation(MID_WITH_LOCATIONS, ZERO, JAPANESE, exampleContent);
            backend.setSubtitles(subtitles);
        }
        {
            Subtitles subtitles = Subtitles.webvttTranslation(MID_WITH_LOCATIONS, ZERO, CHINESE, exampleContent);
            backend.setSubtitles(subtitles);
        }
    }


    @Test
    @Order(2)
    void checkArrivedInBackend() {
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 3));


        waitUntil(ACCEPTABLE_DURATION_BACKEND,
            MID_WITH_LOCATIONS + " has " + JAPANESE_TRANSLATION + " and " + CHINESE_TRANSLATION,
            () -> {
                MediaObject mo = backend.getFull(MID_WITH_LOCATIONS);
                List<AvailableSubtitles> availableSubtitles = mo.getAvailableSubtitles();
                availableSubtitles.removeIf(a -> SubtitlesWorkflow.DELETEDS.contains(a.getWorkflow()));
                log.info("{}", availableSubtitles);
                return availableSubtitles.containsAll(Arrays.asList(JAPANESE_TRANSLATION, CHINESE_TRANSLATION));
            });
        arrivedInBackend = true;

    }

    @Test
    @Order(3)
    void waitForCuesAvailableInFrontend() {
        waitForCuesAvailableInFrontend(JAPANESE, CHINESE);
    }


    @Test
    @Order(4)
    void waitForInMediaFrontend() {
        assumeThat(firstTitle).isNotNull();
        assumeTrue(arrivedInBackend);

        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has " + JAPANESE_TRANSLATION,
            () -> mediaUtil.findByMid(MID_WITH_LOCATIONS).getAvailableSubtitles().containsAll(Arrays.asList(JAPANESE_TRANSLATION, CHINESE_TRANSLATION))
        );
    }

    @Test
    @Order(5)
    void revokeLocations() {
        Instant now = Instant.now();
        ProgramUpdate o = backend.get(MID_WITH_LOCATIONS);
        o.getLocations().forEach(l -> l.setPublishStopInstant(now));
        o.getPredictions().forEach(pu -> pu.setPublishStop(now));
        backend.set(o);
    }

    @Test
    @Order(6)
    void waitForCuesDisappearedInFrontendAfterLocationsRevoked() {
        assumeThat(firstTitle).isNotNull();
        assumeTrue(arrivedInBackend);

        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has no publishable locations",
            () -> mediaUtil.load(MID_WITH_LOCATIONS)[0].getLocations().stream().noneMatch(TrackableObject::isPublishable));

        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
                MID_WITH_LOCATIONS + " has no subtitles in frontend for JAPAN",
            () -> MediaRestClientUtils.loadOrNull(mediaUtil.getClients().getSubtitlesRestService(), MID_WITH_LOCATIONS, Locale.JAPAN) == null);
        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
                MID_WITH_LOCATIONS + " has no subtitles in frontend for CHINESE",
            () -> MediaRestClientUtils.loadOrNull(mediaUtil.getClients().getSubtitlesRestService(), MID_WITH_LOCATIONS, CHINESE) == null);
    }

    @Test
    @Order(7)
    void publishLocations() {
        assumeTrue(arrivedInBackend);

        ProgramUpdate o = backend.get(MID_WITH_LOCATIONS);
        o.getLocations().forEach(l -> l.setPublishStopInstant(null));
        backend.set(o);
    }

    @Test
    @Order(8)
    void waitForCuesAvailableInFrontendAfterPublishLocations() {
        waitForCuesAvailableInFrontend(JAPANESE, CHINESE);
    }


    @Test
    @Order(9)
    void updateOffset() {
        backend.getBackendRestService().setSubtitlesOffset(
            MID_WITH_LOCATIONS, JAPANESE, TRANSLATION,
            Duration.ofSeconds(2).plusMillis(200), null, null);
    }
    @Test
    @Order(10)
    void checkUpdateOffset() {
        backend.getBackendRestService().setSubtitlesOffset(MID_WITH_LOCATIONS, JAPANESE, TRANSLATION, Duration.ofMinutes(1), null, null);
        PeekingIterator<StandaloneCue> cueIterator = waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + "/" + JAPANESE_TRANSLATION + "[0] has start zero",
        () -> {
            clearCaches();
            try {
                return Iterators.peekingIterator(
                    SubtitlesUtil.standaloneStream(
                        MediaRestClientUtils.loadOrNull(
                            mediaUtil.getClients().getSubtitlesRestService(),
                            MID_WITH_LOCATIONS, JAPANESE), false, false).iterator()
                );
            } catch (IOException ioe) {
                log.warn(ioe.getMessage());
                return null;
            }
        }
            , (pi) -> {
                if (pi == null ||  !pi.hasNext()) {
                    log.info("No results yet");
                    return false;
                }
                StandaloneCue peek = pi.peek();
                Duration start = peek.getStart();
                log.info("Start of first cue is now {}", start);
                return start.isZero();
            }
        );
        assertThat(cueIterator.peek().getStart()).isEqualTo(ZERO);
    }

     @Test
     @Order(12)
     void deleteJapanese() {
         try(Response response = backend.getBackendRestService()
             .deleteSubtitles(MID_WITH_LOCATIONS, JAPANESE, TRANSLATION, true, null)) {
             log.info("{}", response);
         }
    }

    @Test
    @Order(13)
    void checkDeleteJapaneseFrontend() {
        assumeThat(firstTitle).isNotNull();
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 3));

        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has no " + JAPANESE_TRANSLATION,
            () -> ! mediaUtil.findByMid(MID_WITH_LOCATIONS).getAvailableSubtitles().contains(JAPANESE_TRANSLATION));


        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has no subtitles for JAPAN",
            () -> MediaRestClientUtils.loadOrNull(mediaUtil.getClients().getSubtitlesRestService(), MID_WITH_LOCATIONS, Locale.JAPAN) == null);

        // the chinese ones still need to be there
        waitForCuesAvailableInFrontend(CHINESE);

    }

    @Test
    @Order(100)
    void cleanup() {
        try(Response response = backend.getBackendRestService()
            .deleteSubtitles(MID_WITH_LOCATIONS, JAPANESE, TRANSLATION, true, null)) {
            log.info("{}", response);
        }
        try(Response response = backend.getBackendRestService()
            .deleteSubtitles(MID_WITH_LOCATIONS, CHINESE, TRANSLATION, true, null)) {
            log.info("{}", response);
        }
    }

    @Test
    @Order(101)
    void checkCleanup() {
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 3));

        waitUntil(ACCEPTABLE_DURATION_BACKEND,
            MID_WITH_LOCATIONS + " has no " + JAPANESE_TRANSLATION ,
            () -> {
                MediaObject mo = backend.getFull(MID_WITH_LOCATIONS);
                Optional<AvailableSubtitles> ja = mo.getAvailableSubtitles().stream().filter(a -> a.equals(JAPANESE_TRANSLATION)).findFirst();
                return ja.isPresent() && ja.get().getWorkflow() == SubtitlesWorkflow.DELETED;
            });

        waitUntil(ACCEPTABLE_DURATION_BACKEND,
            MID_WITH_LOCATIONS + " has no " + CHINESE_TRANSLATION ,
            () -> {
                MediaObject mo = backend.getFull(MID_WITH_LOCATIONS);
                Optional<AvailableSubtitles> zh = mo.getAvailableSubtitles().stream().filter(a -> a.equals(CHINESE_TRANSLATION)).findFirst();
                return zh.isPresent() && zh.get().getWorkflow() == SubtitlesWorkflow.DELETED;
            });


    }

    @Test
    @Order(102)
    void checkCleanupFrontend() {
        assumeThat(firstTitle).isNotNull();
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 3));
        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has no " + JAPANESE_TRANSLATION,
            () -> ! mediaUtil.findByMid(MID_WITH_LOCATIONS).getAvailableSubtitles().contains(JAPANESE_TRANSLATION));

        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has no " + CHINESE_TRANSLATION,
            () -> ! mediaUtil.findByMid(MID_WITH_LOCATIONS).getAvailableSubtitles().contains(CHINESE_TRANSLATION));


        waitUntil(ACCEPTABLE_DURATION_FRONTEND,
            MID_WITH_LOCATIONS + " has no subtitles for Chinese",
            () -> MediaRestClientUtils.loadOrNull(mediaUtil.getClients().getSubtitlesRestService(), MID_WITH_LOCATIONS, CHINESE) == null);
    }


     void waitForCuesAvailableInFrontend(Locale... locales) {
         assumeThat(firstTitle).isNotNull();
         assumeTrue(arrivedInBackend);
         for(Locale locale : locales) {
             PeekingIterator<StandaloneCue> cueIterator = waitUntil(ACCEPTABLE_DURATION_FRONTEND,
                 MID_WITH_LOCATIONS + "/" + locale + "[0]=" + firstTitle,
                 () -> {
                     clearCaches();
                     try {
                         return Iterators.peekingIterator(
                             SubtitlesUtil.standaloneStream(
                                 MediaRestClientUtils.loadOrNull(mediaUtil.getClients().getSubtitlesRestService(),
                                     MID_WITH_LOCATIONS, locale), false, false).iterator()
                         );
                     } catch (IOException ioe) {
                         log.warn(ioe.getMessage());
                         return null;
                     }
                 }
                 , (pi) -> {
                     if (pi == null ||  !pi.hasNext()) {
                         log.info("No results yet");
                         return false;
                     }
                     StandaloneCue peek = pi.peek();
                     String content = peek.getContent();
                     if (!content.equals(firstTitle)) {
                         log.info("Found cue {} != {} yet", content, firstTitle);
                         return false;
                     }
                     return true;
                 }
             );
             assertThat(cueIterator).toIterable().hasSize(3);
         }
    }


}
