package ua.nure.kryvko.hikeway.data.pois.remote

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.data.services.backend.PoiService
import ua.nure.kryvko.hikeway.data.services.network.RetrofitFactory

class RemotePointOfInterestRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemotePointOfInterestRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val gson = Gson()
        val service = RetrofitFactory.create(server.url("/").toString(), gson)
            .create(PoiService::class.java)
        repository = RemotePointOfInterestRepository(service, gson)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loadsNearbyPoisAsLightweightSummaries() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items": [
                    {
                      "id": 7,
                      "name": "Spring",
                      "description": "Fresh water",
                      "longitude": 24.1,
                      "latitude": 49.8,
                      "ownerDisplayName": "Roman",
                      "ownedByCurrentUser": false,
                      "distanceMeters": 125.5
                    }
                  ],
                  "page": 0,
                  "size": 50,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """.trimIndent()
            )
        )

        val pois = repository.getNearby(GeoPoint(24.0, 49.7), 10_000.0)

        assertEquals(1, pois.size)
        assertEquals("Spring", pois.single().name)
        assertEquals(125.5, pois.single().distanceMeters ?: 0.0, 0.01)
        val request = server.takeRequest()
        assertEquals("/pois/nearby?longitude=24.0&latitude=49.7&radiusMeters=10000.0&page=0&size=50", request.path)
    }

    @Test
    fun loadsDetailWithComments() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": 8,
                  "name": "Lookout",
                  "description": "Open view",
                  "longitude": 24.2,
                  "latitude": 49.9,
                  "ownerId": "owner-1",
                  "ownerDisplayName": "Owner",
                  "ownedByCurrentUser": true,
                  "averageRating": 4.5,
                  "ratingCount": 2,
                  "userRating": 5,
                  "photos": [
                    {
                      "id": 3,
                      "contributorId": "owner-1",
                      "contributorDisplayName": "Owner",
                      "ownedByCurrentUser": true,
                      "url": "https://cdn/photo.jpg",
                      "contentType": "image/jpeg",
                      "sizeBytes": 10,
                      "caption": "View",
                      "createdAt": "2026-06-26T10:00:00Z"
                    }
                  ],
                  "createdAt": "2026-06-26T09:00:00Z",
                  "updatedAt": "2026-06-26T10:00:00Z"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items": [
                    {
                      "id": 4,
                      "authorId": "user-1",
                      "authorDisplayName": "Hiker",
                      "ownedByCurrentUser": false,
                      "text": "Nice stop",
                      "createdAt": "2026-06-26T11:00:00Z",
                      "updatedAt": "2026-06-26T11:00:00Z"
                    }
                  ],
                  "page": 0,
                  "size": 50,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """.trimIndent()
            )
        )

        val detail = repository.getDetail(8)

        assertEquals("Lookout", detail.name)
        assertEquals(1, detail.photos.size)
        assertEquals(1, detail.comments.size)
        assertTrue(detail.ownedByCurrentUser)
        assertEquals("/pois/8", server.takeRequest().path)
        assertEquals("/pois/8/comments?page=0&size=50", server.takeRequest().path)
    }

    @Test
    fun createsPoi() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": 12,
                  "name": "New spring",
                  "description": "Fresh water",
                  "longitude": 24.3,
                  "latitude": 49.7,
                  "ownerId": "me",
                  "ownerDisplayName": "Me",
                  "ownedByCurrentUser": true,
                  "averageRating": 0.0,
                  "ratingCount": 0,
                  "userRating": null,
                  "photos": [],
                  "createdAt": "2026-06-27T10:00:00Z",
                  "updatedAt": "2026-06-27T10:00:00Z"
                }
                """.trimIndent()
            )
        )

        val poi = repository.create(
            name = "New spring",
            description = "Fresh water",
            location = GeoPoint(24.3, 49.7),
        )

        assertEquals(12, poi.id)
        assertEquals("New spring", poi.name)
        assertTrue(poi.ownedByCurrentUser)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/pois", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"name\":\"New spring\""))
        assertTrue(body.contains("\"longitude\":24.3"))
        assertTrue(body.contains("\"latitude\":49.7"))
    }

    @Test
    fun submitsRatingAndCommentMutations() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"averageRating":4.0,"ratingCount":3,"userRating":4}"""
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": 11,
                  "authorId": "me",
                  "authorDisplayName": "Me",
                  "ownedByCurrentUser": true,
                  "text": "Good place",
                  "createdAt": "2026-06-26T11:00:00Z",
                  "updatedAt": "2026-06-26T11:00:00Z"
                }
                """.trimIndent()
            )
        )

        val rating = repository.submitRating(9, 4)
        val comment = repository.addComment(9, "Good place")

        assertEquals(4, rating.userRating)
        assertEquals("Good place", comment.text)
        val ratingRequest = server.takeRequest()
        val commentRequest = server.takeRequest()
        assertEquals("PUT", ratingRequest.method)
        assertEquals("/pois/9/rating", ratingRequest.path)
        assertTrue(ratingRequest.body.readUtf8().contains("\"score\":4"))
        assertEquals("POST", commentRequest.method)
        assertEquals("/pois/9/comments", commentRequest.path)
    }

    @Test
    fun uploadsPhotoThenFinalizesIt() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "photoId": 21,
                  "objectKey": "pois/9/21.jpg",
                  "uploadUrl": "${server.url("/upload/21")}",
                  "expiresAt": "2026-06-26T12:00:00Z",
                  "contentType": "image/jpeg",
                  "sizeBytes": 3
                }
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": 21,
                  "contributorId": "me",
                  "contributorDisplayName": "Me",
                  "ownedByCurrentUser": true,
                  "url": "https://cdn/21.jpg",
                  "contentType": "image/jpeg",
                  "sizeBytes": 3,
                  "caption": "Trail",
                  "createdAt": "2026-06-26T12:01:00Z"
                }
                """.trimIndent()
            )
        )

        val photo = repository.uploadPhoto(
            9,
            PoiPhotoUpload(byteArrayOf(1, 2, 3), "image/jpeg", "Trail")
        )

        assertEquals(21, photo.id)
        assertEquals("Trail", photo.caption)
        assertEquals("/pois/9/photos/uploads", server.takeRequest().path)
        val uploadRequest = server.takeRequest()
        assertEquals("PUT", uploadRequest.method)
        assertEquals("/upload/21", uploadRequest.path)
        assertEquals(3, uploadRequest.body.size)
        val finalizeRequest = server.takeRequest()
        assertEquals("/pois/9/photos", finalizeRequest.path)
        assertTrue(finalizeRequest.body.readUtf8().contains("\"photoId\":21"))
    }
}
