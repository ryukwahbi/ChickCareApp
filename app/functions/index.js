/**
 * Firebase Cloud Functions for ChickCare App
 * 
 * This file contains Cloud Functions for sending announcement notifications
 * to all users or specific users.
 * 
 * Setup Instructions:
 * 1. Install Node.js (v18 or higher)
 * 2. Navigate to the functions directory: cd functions
 * 3. Install dependencies: npm install
 * 4. Deploy: npm run deploy
 * 
 * Usage:
 * - Call sendAnnouncementToAllUsers() via HTTP request or Firebase Console
 * - Or use the sendAnnouncementToUser() for targeted announcements
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.firestore();

/**
 * Helper function to verify Firebase Auth token and check if user is admin
 * @param {string} authHeader - Authorization header from request
 * @returns {Promise<{uid: string, isAdmin: boolean}>} - User ID and admin status
 */
async function verifyAuthToken(authHeader) {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw new Error('Missing or invalid authorization header');
  }

  const token = authHeader.split('Bearer ')[1];
  
  try {
    // Verify the Firebase Auth token
    const decodedToken = await admin.auth().verifyIdToken(token);
    const uid = decodedToken.uid;

    // Check if user is admin (you can customize this logic)
    // Option 1: Check if user has 'role' field set to 'admin' in Firestore
    const userDoc = await db.collection('users').doc(uid).get();
    const userData = userDoc.data();
    const isAdmin = userData?.role === 'admin' || userData?.isAdmin === true;

    return { uid, isAdmin };
  } catch (error) {
    throw new Error('Invalid or expired token');
  }
}

/**
 * Helper function to set CORS headers (restricted to specific origins)
 * @param {object} res - Express response object
 * @param {string} origin - Request origin
 */
function setCORSHeaders(res, origin) {
  // SECURITY: Only allow specific origins (not wildcard *)
  // Add your app's domain here when you deploy
  const allowedOrigins = [
    'http://localhost:3000',  // For local development
    'https://chickcare-ab7bc.web.app',  // Firebase Hosting (if you use it)
    'https://chickcare-ab7bc.firebaseapp.com',  // Firebase Hosting alternative
    // Add your production domain here
  ];

  // Check if origin is allowed
  if (origin && allowedOrigins.includes(origin)) {
    res.set('Access-Control-Allow-Origin', origin);
  } else {
    // For mobile apps, you might want to allow all origins
    // But restrict by checking the Authorization header instead
    // For now, we'll be strict and only allow known origins
    res.set('Access-Control-Allow-Origin', allowedOrigins[0] || 'null');
  }

  res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.set('Access-Control-Allow-Credentials', 'true');
}

/**
 * Send announcement notification to all users
 * 
 * HTTP Endpoint: POST https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/sendAnnouncementToAllUsers
 * 
 * Request Body (JSON):
 * {
 *   "title": "App Update Available",
 *   "message": "We've added new features to improve your chicken health monitoring experience!"
 * }
 * 
 * Example using curl:
 * curl -X POST https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/sendAnnouncementToAllUsers \
 *   -H "Content-Type: application/json" \
 *   -d '{"title":"App Update","message":"New features available!"}'
 */
exports.sendAnnouncementToAllUsers = functions.https.onRequest(async (req, res) => {
  // Set CORS headers (restricted to allowed origins)
  setCORSHeaders(res, req.headers.origin);

  // Handle preflight request
  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  // Only allow POST requests
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed. Use POST.' });
    return;
  }

  // SECURITY: Verify authentication and admin status
  let authInfo;
  try {
    authInfo = await verifyAuthToken(req.headers.authorization);
  } catch (error) {
    res.status(401).json({ 
      error: 'Unauthorized',
      message: 'Valid Firebase Auth token required. Please include Authorization: Bearer <token> header.'
    });
    return;
  }

  // SECURITY: Only admins can send announcements to all users
  if (!authInfo.isAdmin) {
    res.status(403).json({ 
      error: 'Forbidden',
      message: 'Admin privileges required to send announcements to all users.'
    });
    return;
  }

  try {
    const { title, message } = req.body;

    // Validate input
    if (!title || !message) {
      res.status(400).json({ 
        error: 'Missing required fields',
        message: 'Both "title" and "message" are required.' 
      });
      return;
    }

    // Get all users
    const usersSnapshot = await db.collection('users').get();
    
    if (usersSnapshot.empty) {
      res.status(200).json({ 
        success: true,
        message: 'No users found to send announcements to.',
        sentCount: 0
      });
      return;
    }

    // Prepare notification data
    // Use Date.now() for emulator compatibility (Firestore will convert to Timestamp)
    const timestamp = Date.now();
    
    const notificationData = {
      type: 'ANNOUNCEMENT',
      title: title,
      message: message,
      timestamp: timestamp,
      isRead: false,
      senderId: null,
      senderName: null,
      relatedEntityId: null,
      actionRequired: false
    };

    // Send notification to each user
    const batch = db.batch();
    let count = 0;

    usersSnapshot.forEach((userDoc) => {
      const notificationRef = db
        .collection('users')
        .doc(userDoc.id)
        .collection('notifications')
        .doc();
      
      batch.set(notificationRef, notificationData);
      count++;
    });

    // Commit batch write
    await batch.commit();

    console.log(`Successfully sent announcement to ${count} users`);
    
    res.status(200).json({
      success: true,
      message: `Announcement sent successfully to ${count} users.`,
      sentCount: count,
      title: title,
      message: message
    });

  } catch (error) {
    console.error('Error sending announcement:', error);
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

/**
 * Send announcement notification to a specific user
 * 
 * HTTP Endpoint: POST https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/sendAnnouncementToUser
 * 
 * Request Body (JSON):
 * {
 *   "userId": "user123",
 *   "title": "Personal Notification",
 *   "message": "This is a targeted announcement for you."
 * }
 */
exports.sendAnnouncementToUser = functions.https.onRequest(async (req, res) => {
  // Set CORS headers (restricted to allowed origins)
  setCORSHeaders(res, req.headers.origin);

  // Handle preflight request
  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  // Only allow POST requests
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed. Use POST.' });
    return;
  }

  // SECURITY: Verify authentication
  let authInfo;
  try {
    authInfo = await verifyAuthToken(req.headers.authorization);
  } catch (error) {
    res.status(401).json({ 
      error: 'Unauthorized',
      message: 'Valid Firebase Auth token required. Please include Authorization: Bearer <token> header.'
    });
    return;
  }

  try {
    const { userId, title, message } = req.body;
    
    // SECURITY: Users can only send announcements to themselves, or admins can send to anyone
    if (userId !== authInfo.uid && !authInfo.isAdmin) {
      res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only send announcements to yourself, or you need admin privileges.'
      });
      return;
    }

    // Validate input
    if (!userId || !title || !message) {
      res.status(400).json({ 
        error: 'Missing required fields',
        message: 'All fields "userId", "title", and "message" are required.' 
      });
      return;
    }

    // Check if user exists
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      res.status(404).json({ 
        error: 'User not found',
        message: `User with ID ${userId} does not exist.` 
      });
      return;
    }

    // Prepare notification data
    // Use Date.now() for emulator compatibility (Firestore will convert to Timestamp)
    const timestamp = Date.now();
    
    const notificationData = {
      type: 'ANNOUNCEMENT',
      title: title,
      message: message,
      timestamp: timestamp,
      isRead: false,
      senderId: null,
      senderName: null,
      relatedEntityId: null,
      actionRequired: false
    };

    // Add notification to user's notifications collection
    await db
      .collection('users')
      .doc(userId)
      .collection('notifications')
      .add(notificationData);

    console.log(`Successfully sent announcement to user ${userId}`);
    
    res.status(200).json({
      success: true,
      message: `Announcement sent successfully to user ${userId}.`,
      userId: userId,
      title: title,
      message: message
    });

  } catch (error) {
    console.error('Error sending announcement:', error);
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

/**
 * Scheduled function to send periodic announcements (optional)
 * 
 * This function runs daily at 9:00 AM UTC
 * You can customize the schedule using cron syntax
 * 
 * To enable, uncomment this function and deploy
 */
/*
exports.sendDailyAnnouncement = functions.pubsub.schedule('0 9 * * *')
  .timeZone('UTC')
  .onRun(async (context) => {
    try {
      const title = "Daily Health Tip";
      const message = "Remember to check your chickens' health regularly using our detection feature!";
      
      const usersSnapshot = await db.collection('users').get();
      
      if (usersSnapshot.empty) {
        console.log('No users found');
        return null;
      }

      const batch = db.batch();
      let count = 0;

      usersSnapshot.forEach((userDoc) => {
        const notificationRef = db
          .collection('users')
          .doc(userDoc.id)
          .collection('notifications')
          .doc();
        
        batch.set(notificationRef, {
          type: 'ANNOUNCEMENT',
          title: title,
          message: message,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false,
          senderId: null,
          senderName: null,
          relatedEntityId: null,
          actionRequired: false
        });
        count++;
      });

      await batch.commit();
      console.log(`Sent daily announcement to ${count} users`);
      
      return null;
    } catch (error) {
      console.error('Error in scheduled announcement:', error);
      return null;
    }
  });
*/

/**
 * Example: Send system update notification
 * 
 * HTTP Endpoint: POST https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/sendSystemUpdate
 */
exports.sendSystemUpdate = functions.https.onRequest(async (req, res) => {
  // Set CORS headers (restricted to allowed origins)
  setCORSHeaders(res, req.headers.origin);

  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed. Use POST.' });
    return;
  }

  // SECURITY: Verify authentication and admin status
  let authInfo;
  try {
    authInfo = await verifyAuthToken(req.headers.authorization);
  } catch (error) {
    res.status(401).json({ 
      error: 'Unauthorized',
      message: 'Valid Firebase Auth token required. Please include Authorization: Bearer <token> header.'
    });
    return;
  }

  // SECURITY: Only admins can send system updates
  if (!authInfo.isAdmin) {
    res.status(403).json({ 
      error: 'Forbidden',
      message: 'Admin privileges required to send system updates.'
    });
    return;
  }

  try {
    const { title, message, userId } = req.body;

    if (!title || !message) {
      res.status(400).json({ 
        error: 'Missing required fields',
        message: 'Both "title" and "message" are required.' 
      });
      return;
    }

    // Use Date.now() for emulator compatibility (Firestore will convert to Timestamp)
    const timestamp = Date.now();
    
    const notificationData = {
      type: 'SYSTEM_UPDATE',
      title: title,
      message: message,
      timestamp: timestamp,
      isRead: false,
      senderId: null,
      senderName: null,
      relatedEntityId: null,
      actionRequired: false
    };

    // If userId is provided, send to specific user, otherwise send to all
    if (userId) {
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        res.status(404).json({ error: 'User not found' });
        return;
      }
      
      await db
        .collection('users')
        .doc(userId)
        .collection('notifications')
        .add(notificationData);
      
      res.status(200).json({
        success: true,
        message: `System update sent to user ${userId}`,
        userId: userId
      });
    } else {
      // Send to all users
      const usersSnapshot = await db.collection('users').get();
      const batch = db.batch();
      let count = 0;

      usersSnapshot.forEach((userDoc) => {
        const notificationRef = db
          .collection('users')
          .doc(userDoc.id)
          .collection('notifications')
          .doc();
        batch.set(notificationRef, notificationData);
        count++;
      });

      await batch.commit();
      
      res.status(200).json({
        success: true,
        message: `System update sent to ${count} users`,
        sentCount: count
      });
    }

  } catch (error) {
    console.error('Error sending system update:', error);
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

/**
 * Send chat message notification via FCM
 * 
 * HTTP Endpoint: POST https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/sendChatNotification
 * 
 * Request Body (JSON):
 * {
 *   "token": "fcm_token_here",
 *   "title": "Sender Name",
 *   "body": "Message preview",
 *   "data": {
 *     "type": "CHAT_MESSAGE",
 *     "senderId": "sender123",
 *     "receiverId": "receiver456",
 *     "senderName": "John Doe",
 *     "senderPhotoUrl": "https://...",
 *     "message": "Hello!",
 *     "messageType": "text"
 *   }
 * }
 */
exports.sendChatNotification = functions.https.onCall(async (data, context) => {
  try {
    const { token, title, body, data: notificationData } = data;

    if (!token || !title || !body) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Missing required fields: token, title, body'
      );
    }

    // Prepare FCM message
    const message = {
      token: token,
      notification: {
        title: title,
        body: body,
      },
      data: {
        type: notificationData?.type || 'CHAT_MESSAGE',
        senderId: notificationData?.senderId || '',
        receiverId: notificationData?.receiverId || '',
        senderName: notificationData?.senderName || title,
        senderPhotoUrl: notificationData?.senderPhotoUrl || '',
        message: notificationData?.message || body,
        messageType: notificationData?.messageType || 'text',
      },
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          channelId: 'chickcare_chat_notifications',
          priority: 'high',
        },
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1,
          },
        },
      },
    };

    // Send notification
    const response = await admin.messaging().send(message);
    console.log('Successfully sent chat notification:', response);

    return {
      success: true,
      messageId: response,
    };
  } catch (error) {
    console.error('Error sending chat notification:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to send notification',
      error.message
    );
  }
});

