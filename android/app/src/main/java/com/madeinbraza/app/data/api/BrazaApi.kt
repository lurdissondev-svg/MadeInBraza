package com.madeinbraza.app.data.api

import com.madeinbraza.app.data.model.ApproveUserResponse
import com.madeinbraza.app.data.model.AuthResponse
import com.madeinbraza.app.data.model.AvatarDeleteResponse
import com.madeinbraza.app.data.model.AvatarUploadResponse
import com.madeinbraza.app.data.model.AvailableSharesResponse
import com.madeinbraza.app.data.model.BannedUsersResponse
import com.madeinbraza.app.data.model.CreateEventRequest
import com.madeinbraza.app.data.model.CreatePartyRequest
import com.madeinbraza.app.data.model.CreatePartyResponse
import com.madeinbraza.app.data.model.JoinPartyRequest
import com.madeinbraza.app.data.model.CreateSiegeWarResponse
import com.madeinbraza.app.data.model.SiegeWarHistoryResponse
import com.madeinbraza.app.data.model.CurrentSiegeWarResponse
import com.madeinbraza.app.data.model.FcmTokenRequest
import com.madeinbraza.app.data.model.CreateEventResponse
import com.madeinbraza.app.data.model.EventsResponse
import com.madeinbraza.app.data.model.JoinPartyResponse
import com.madeinbraza.app.data.model.LoginRequest
import com.madeinbraza.app.data.model.MemberProfileResponse
import com.madeinbraza.app.data.model.MembersResponse
import com.madeinbraza.app.data.model.PartiesResponse
import com.madeinbraza.app.data.model.ProfileResponse
import com.madeinbraza.app.data.model.UpdateProfileRequest
import com.madeinbraza.app.data.model.UpdateProfileResponse
import com.madeinbraza.app.data.model.MessagesResponse
import com.madeinbraza.app.data.model.PendingUsersResponse
import com.madeinbraza.app.data.model.RegisterRequest
import com.madeinbraza.app.data.model.SendMessageRequest
import com.madeinbraza.app.data.model.SendMessageResponse
import com.madeinbraza.app.data.model.StatusResponse
import com.madeinbraza.app.data.model.SubmitSWResponseRequest
import com.madeinbraza.app.data.model.SubmitSWResponseResponse
import com.madeinbraza.app.data.model.SuccessResponse
import com.madeinbraza.app.data.model.SWResponsesResponse
import com.madeinbraza.app.data.model.Channel
import com.madeinbraza.app.data.model.ChannelMembersResponse
import com.madeinbraza.app.data.model.ChannelMessage
import com.madeinbraza.app.data.model.SendChannelMessageRequest
import com.madeinbraza.app.data.model.AnnouncementsResponse
import com.madeinbraza.app.data.model.CreateAnnouncementRequest
import com.madeinbraza.app.data.model.CreateAnnouncementResponse
import com.madeinbraza.app.data.model.ChangePasswordRequest
import com.madeinbraza.app.data.model.ForgotPasswordRequest
import com.madeinbraza.app.data.model.ForgotPasswordResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface BrazaApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/status")
    suspend fun checkStatus(@Header("Authorization") token: String): Response<StatusResponse>

    @POST("auth/fcm-token")
    suspend fun registerFcmToken(
        @Header("Authorization") token: String,
        @Body request: FcmTokenRequest
    ): Response<SuccessResponse>

    @PUT("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<SuccessResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ForgotPasswordResponse>

    @GET("users/members")
    suspend fun getMembers(@Header("Authorization") token: String): Response<MembersResponse>

    @GET("users/pending")
    suspend fun getPendingUsers(@Header("Authorization") token: String): Response<PendingUsersResponse>

    @POST("users/{id}/approve")
    suspend fun approveUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<ApproveUserResponse>

    @POST("users/{id}/reject")
    suspend fun rejectUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<SuccessResponse>

    @POST("users/{id}/ban")
    suspend fun banUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<ApproveUserResponse>

    @POST("users/{id}/promote")
    suspend fun promoteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<ApproveUserResponse>

    @POST("users/{id}/demote")
    suspend fun demoteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<ApproveUserResponse>

    @GET("users/banned")
    suspend fun getBannedUsers(@Header("Authorization") token: String): Response<BannedUsersResponse>

    @POST("users/{id}/unban")
    suspend fun unbanUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<ApproveUserResponse>

    @GET("users/{id}/profile")
    suspend fun getMemberProfile(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<MemberProfileResponse>

    // Chat endpoints
    @GET("chat/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null
    ): Response<MessagesResponse>

    @POST("chat/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    // Events endpoints
    @GET("events")
    suspend fun getEvents(
        @Header("Authorization") token: String
    ): Response<EventsResponse>

    @POST("events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Body request: CreateEventRequest
    ): Response<CreateEventResponse>

    @DELETE("events/{id}")
    suspend fun deleteEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String
    ): Response<SuccessResponse>

    @POST("events/{id}/join")
    suspend fun joinEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String
    ): Response<SuccessResponse>

    @POST("events/{id}/leave")
    suspend fun leaveEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String
    ): Response<SuccessResponse>

    // Profile endpoints
    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>

    @PUT("profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @Multipart
    @POST("profile/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part
    ): Response<AvatarUploadResponse>

    @DELETE("profile/avatar")
    suspend fun deleteAvatar(
        @Header("Authorization") token: String
    ): Response<AvatarDeleteResponse>

    // Party endpoints
    @GET("parties")
    suspend fun getGlobalParties(
        @Header("Authorization") token: String
    ): Response<PartiesResponse>

    @POST("parties")
    suspend fun createGlobalParty(
        @Header("Authorization") token: String,
        @Body request: CreatePartyRequest
    ): Response<CreatePartyResponse>

    @GET("parties/event/{eventId}")
    suspend fun getPartiesByEvent(
        @Header("Authorization") token: String,
        @Path("eventId") eventId: String
    ): Response<PartiesResponse>

    @POST("parties/event/{eventId}")
    suspend fun createParty(
        @Header("Authorization") token: String,
        @Path("eventId") eventId: String,
        @Body request: CreatePartyRequest
    ): Response<CreatePartyResponse>

    @PATCH("parties/{partyId}")
    suspend fun updateParty(
        @Header("Authorization") token: String,
        @Path("partyId") partyId: String,
        @Body request: CreatePartyRequest
    ): Response<CreatePartyResponse>

    @DELETE("parties/{partyId}")
    suspend fun deleteParty(
        @Header("Authorization") token: String,
        @Path("partyId") partyId: String
    ): Response<SuccessResponse>

    @POST("parties/{partyId}/join")
    suspend fun joinParty(
        @Header("Authorization") token: String,
        @Path("partyId") partyId: String,
        @Body request: JoinPartyRequest
    ): Response<JoinPartyResponse>

    @POST("parties/{partyId}/leave")
    suspend fun leaveParty(
        @Header("Authorization") token: String,
        @Path("partyId") partyId: String
    ): Response<SuccessResponse>

    // Siege War endpoints
    @GET("siege-war/current")
    suspend fun getCurrentSiegeWar(
        @Header("Authorization") token: String
    ): Response<CurrentSiegeWarResponse>

    @POST("siege-war")
    suspend fun createSiegeWar(
        @Header("Authorization") token: String
    ): Response<CreateSiegeWarResponse>

    @POST("siege-war/{siegeWarId}/respond")
    suspend fun submitSWResponse(
        @Header("Authorization") token: String,
        @Path("siegeWarId") siegeWarId: String,
        @Body request: SubmitSWResponseRequest
    ): Response<SubmitSWResponseResponse>

    @GET("siege-war/{siegeWarId}/responses")
    suspend fun getSWResponses(
        @Header("Authorization") token: String,
        @Path("siegeWarId") siegeWarId: String
    ): Response<SWResponsesResponse>

    @GET("siege-war/{siegeWarId}/available-shares")
    suspend fun getAvailableShares(
        @Header("Authorization") token: String,
        @Path("siegeWarId") siegeWarId: String
    ): Response<AvailableSharesResponse>

    @POST("siege-war/{siegeWarId}/close")
    suspend fun closeSiegeWar(
        @Header("Authorization") token: String,
        @Path("siegeWarId") siegeWarId: String
    ): Response<CreateSiegeWarResponse>

    @GET("siege-war/history")
    suspend fun getSiegeWarHistory(
        @Header("Authorization") token: String
    ): Response<SiegeWarHistoryResponse>

    // Channel endpoints
    @GET("channels")
    suspend fun getChannels(
        @Header("Authorization") token: String
    ): Response<List<Channel>>

    @POST("channels/setup")
    suspend fun setupDefaultChannels(
        @Header("Authorization") token: String
    ): Response<SuccessResponse>

    @GET("channels/{channelId}/messages")
    suspend fun getChannelMessages(
        @Header("Authorization") token: String,
        @Path("channelId") channelId: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null
    ): Response<List<ChannelMessage>>

    @POST("channels/{channelId}/messages")
    suspend fun sendChannelMessage(
        @Header("Authorization") token: String,
        @Path("channelId") channelId: String,
        @Body request: SendChannelMessageRequest
    ): Response<ChannelMessage>

    @Multipart
    @POST("channels/{channelId}/messages/media")
    suspend fun sendMediaMessage(
        @Header("Authorization") token: String,
        @Path("channelId") channelId: String,
        @Part file: MultipartBody.Part,
        @Part("content") content: RequestBody?
    ): Response<ChannelMessage>

    @GET("channels/{channelId}/members")
    suspend fun getChannelMembers(
        @Header("Authorization") token: String,
        @Path("channelId") channelId: String
    ): Response<ChannelMembersResponse>

    // Announcement endpoints
    @GET("announcements")
    suspend fun getAnnouncements(
        @Header("Authorization") token: String
    ): Response<AnnouncementsResponse>

    @POST("announcements")
    suspend fun createAnnouncement(
        @Header("Authorization") token: String,
        @Body request: CreateAnnouncementRequest
    ): Response<CreateAnnouncementResponse>

    @DELETE("announcements/{id}")
    suspend fun deleteAnnouncement(
        @Header("Authorization") token: String,
        @Path("id") announcementId: String
    ): Response<SuccessResponse>
}
