package com.hh99.nearby.chat.controller;

import com.hh99.nearby.chat.dto.SessionRequestDto;
import io.openvidu.java.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api-sessions")
public class SessionController {

    // OpenVidu object as entrypoint of the SDK
    private OpenVidu openVidu;

    // Collection to pair session names and OpenVidu Session objects
    private Map<String, Session> mapSessions = new ConcurrentHashMap<>();
    // Collection to pair session names and tokens (the inner Map pairs tokens and
    // role associated)
    private Map<String, Map<String, OpenViduRole>> mapSessionNamesTokens = new ConcurrentHashMap<>();

    // URL where our OpenVidu server is listening
    private String OPENVIDU_URL;
    // Secret shared with our OpenVidu server
    private String SECRET;

    public SessionController(@Value("${openvidu.secret}") String secret, @Value("${openvidu.url}") String openviduUrl) {
        this.SECRET = secret;
        this.OPENVIDU_URL = openviduUrl;
        this.openVidu = new OpenVidu(OPENVIDU_URL, SECRET);
    }

    @RequestMapping(value = "/get-token", method = RequestMethod.POST)
//	public ResponseEntity<JSONObject> getToken(@RequestBody String sessionNameParam, HttpSession httpSession)
    public ResponseEntity<?> getToken(@RequestBody SessionRequestDto SessionRequestDto)
            throws ParseException {

//		try {
//			checkUserLogged(httpSession);
//		} catch (Exception e) {
//			return getErrorResponse(e);
//		}
//		System.out.println("Getting a token from OpenVidu Server | {sessionName}=" + sessionNameParam);

//		JSONObject sessionJSON = (JSONObject) new JSONParser().parse(sessionNameParam);
//
//		// The video-call to connect
//		String sessionName = (String) sessionJSON.get("sessionName");
        String sessionName = SessionRequestDto.getSessionName();
        // Role associated to this user
//		OpenViduRole role = LoginController.users.get(httpSession.getAttribute("loggedUser")).role;
        OpenViduRole role = OpenViduRole.PUBLISHER;

        // Optional data to be passed to other users when this user connects to the
        // video-call. In this case, a JSON with the value we stored in the HttpSession
        // object on login
//		String serverData = "{\"serverData\": \"" + httpSession.getAttribute("loggedUser") + "\"}";
        String serverData = "{\"serverData\": \"" + "유저" + "\"}";

        // Build connectionProperties object with the serverData and the role
        ConnectionProperties connectionProperties = new ConnectionProperties.Builder().type(ConnectionType.WEBRTC).data(serverData).role(role).build();

        JSONObject responseJson = new JSONObject();

        if (this.mapSessions.get(sessionName) != null) {
            try {
//				role = OpenViduRole.SUBSCRIBER;
//				ConnectionProperties connectionProperties = new ConnectionProperties.Builder().type(ConnectionType.WEBRTC).data(serverData).role(role).build();
                // Generate a new Connection with the recently created connectionProperties
                String token = this.mapSessions.get(sessionName).createConnection(connectionProperties).getToken();

                // Update our collection storing the new token
                this.mapSessionNamesTokens.get(sessionName).put(token, role);

                // Prepare the response with the token
                responseJson.put(0, token);

                // Return the response to the client
//				return new ResponseEntity<>(responseJson, HttpStatus.OK);
                return ResponseEntity.ok().body(Map.of("msg", "토큰발급 성공", "data", responseJson));
            } catch (OpenViduJavaClientException e1) {
                // If internal error generate an error message and return it to client
                return getErrorResponse(e1);
            } catch (OpenViduHttpException e2) {
                if (404 == e2.getStatus()) {
                    // Invalid sessionId (user left unexpectedly). Session object is not valid
                    // anymore. Clean collections and continue as new session
                    this.mapSessions.remove(sessionName);
                    this.mapSessionNamesTokens.remove(sessionName);
                }
            }
        }


        try {
//			ConnectionProperties connectionProperties = new ConnectionProperties.Builder().type(ConnectionType.WEBRTC).data(serverData).role(role).build();
            // Create a new OpenVidu Session
            Session session = this.openVidu.createSession();
            // Generate a new Connection with the recently created connectionProperties
            String token = session.createConnection(connectionProperties).getToken();

            // Store the session and the token in our collections
            this.mapSessions.put(sessionName, session);
            this.mapSessionNamesTokens.put(sessionName, new ConcurrentHashMap<>());
            this.mapSessionNamesTokens.get(sessionName).put(token, role);

            // Prepare the response with the token
            responseJson.put(0, token);

            // Return the response to the client
//			return new ResponseEntity<>(responseJson, HttpStatus.OK);
            return ResponseEntity.ok().body(Map.of("msg", "토큰발급 성공", "data", responseJson));
        } catch (Exception e) {
            // If error generate an error message and return it to client
            return getErrorResponse(e);
        }
    }

    @RequestMapping(value = "/remove-user", method = RequestMethod.POST)
    public ResponseEntity<JSONObject> removeUser(@RequestBody SessionRequestDto SessionRequestDto)
            throws Exception {
//		try {
//			checkUserLogged(httpSession);
//		} catch (Exception e) {
//			return getErrorResponse(e);
//		}
//		System.out.println("Removing user | {sessionName, token}=" + sessionNameToken);

        // Retrieve the params from BODY
//		JSONObject sessionNameTokenJSON = (JSONObject) new JSONParser().parse(sessionNameToken);
//		String sessionName = (String) sessionNameTokenJSON.get("sessionName");
//		String token = (String) sessionNameTokenJSON.get("token");
        String sessionName = SessionRequestDto.getSessionName();
        String token = SessionRequestDto.getToken();

        // If the session exists
        if (this.mapSessions.get(sessionName) != null && this.mapSessionNamesTokens.get(sessionName) != null) {

            // If the token exists
            if (this.mapSessionNamesTokens.get(sessionName).remove(token) != null) {
                // User left the session
                if (this.mapSessionNamesTokens.get(sessionName).isEmpty()) {
                    // Last user left: session must be removed
                    this.mapSessions.remove(sessionName);
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                // The TOKEN wasn't valid
                System.out.println("Problems in the app server: the TOKEN wasn't valid");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } else {
            // The SESSION does not exist
            System.out.println("Problems in the app server: the SESSION does not exist");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<JSONObject> getErrorResponse(Exception e) {
        JSONObject json = new JSONObject();
        json.put("cause", e.getCause());
        json.put("error", e.getMessage());
        json.put("exception", e.getClass());
        return new ResponseEntity<>(json, HttpStatus.INTERNAL_SERVER_ERROR);
    }

//	private void checkUserLogged(HttpSession httpSession) throws Exception {
//		if (httpSession == null || httpSession.getAttribute("loggedUser") == null) {
//			throw new Exception("User not logged");
//		}
//	}

}
