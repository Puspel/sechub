# SPDX-License-Identifier: MIT

# The image argument needs to be placed on top
ARG BASE_IMAGE
FROM ${BASE_IMAGE}

# The remaining arguments need to be placed after the `FROM`
# See: https://ryandaniels.ca/blog/docker-dockerfile-arg-from-arg-trouble/

# Folders
ARG PDS_FOLDER="/pds"
ARG SCRIPT_FOLDER="/scripts"
ENV TOOL_FOLDER="/tools"
ARG WORKSPACE="/workspace"

# PDS
ENV PDS_VERSION=0.23.1
ARG PDS_CHECKSUM="fb70f3131324f0070631f78229c25168ff50d570a9d481420d095b3bb5aa4a69"

# OWASP ZAP
ARG OWASP_ZAP_CHECKSUM="54750581ec2fd21bd5aa8429b31b09eeb1e750ab6bb7e56f12504251892ccb09"
ARG OWASP_ZAP_VERSION=2.10.0

# Shared volumes
ENV SHARED_VOLUMES="/shared_volumes"
ENV SHARED_VOLUME_UPLOAD_DIR="$SHARED_VOLUMES/uploads"

# non-root user
# using fixed group and user ids
# gosec needs a home directory for the cache
RUN groupadd --gid 2323 zap \
     && useradd --uid 2323 --no-log-init --create-home --gid zap zap

# Update image and install dependencies
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
    apt-get upgrade --assume-yes && \
    apt-get install --assume-yes wget openjdk-11-jre firefox && \
    apt-get clean

# Create script folder
COPY owasp-zap.sh $SCRIPT_FOLDER/owasp-zap.sh
RUN chmod +x $SCRIPT_FOLDER/owasp-zap.sh

# Install OWASP ZAP
RUN mkdir --parents "$TOOL_FOLDER" && \
	cd "$TOOL_FOLDER" && \
	# download latest release of owasp zap
	wget https://github.com/zaproxy/zaproxy/releases/download/v$OWASP_ZAP_VERSION/zaproxy_$OWASP_ZAP_VERSION-1_all.deb && \
	# verify that the checksum and the checksum of the file are same
    echo "$OWASP_ZAP_CHECKSUM zaproxy_$OWASP_ZAP_VERSION-1_all.deb" | sha256sum --check && \
	dpkg -i zaproxy_$OWASP_ZAP_VERSION-1_all.deb && \
	# remove zaproxy deb package
	rm zaproxy_$OWASP_ZAP_VERSION-1_all.deb
	
# Install the Product Delegation Server (PDS)
RUN mkdir --parents "$PDS_FOLDER" && \
    cd "$PDS_FOLDER" && \
    # create checksum file
    echo "$PDS_CHECKSUM  sechub-pds-$PDS_VERSION.jar" > sechub-pds-$PDS_VERSION.jar.sha256sum && \
    # download pds
    wget "https://github.com/Daimler/sechub/releases/download/v$PDS_VERSION-pds/sechub-pds-$PDS_VERSION.jar" && \
    # verify that the checksum and the checksum of the file are same
    sha256sum --check sechub-pds-$PDS_VERSION.jar.sha256sum

# Create shared volumes and upload dir
RUN mkdir --parents "$SHARED_VOLUME_UPLOAD_DIR"

# Copy PDS configfile
COPY pds-config.json /$PDS_FOLDER/pds-config.json

# Copy run script into container
COPY run.sh /run.sh
RUN chmod +x /run.sh

# Create the PDS workspace
WORKDIR "$WORKSPACE"

# Change owner of tool, workspace and pds folder as well as /run.sh
RUN chown --recursive zap:zap $TOOL_FOLDER $SCRIPT_FOLDER $WORKSPACE $PDS_FOLDER $SHARED_VOLUMES /run.sh

# Switch from root to non-root user
USER zap

CMD ["/run.sh"]